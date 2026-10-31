param([string]$Maven, [string]$Source, [string]$Repository, [switch]$SkipBrowser, [int]$Port = 13326, [int]$HttpPort = 18086)
$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $PSScriptRoot
if (-not $Maven) { $Maven = Join-Path $appRoot 'mvnw.cmd' }
if (-not $Repository) { $Repository = Join-Path $appRoot '.build-deps/cache' }
$compose = Join-Path $appRoot 'tests/birthday/compose.yml'
$project = 'nw-birthday-it-' + [Guid]::NewGuid().ToString('N').Substring(0, 10)
$files = Join-Path $appRoot ".local/birthday-test/$project"
New-Item -ItemType Directory -Force "$files/secrets", "$files/keys", "$files/backup" | Out-Null
[IO.File]::WriteAllText("$files/secrets/password", ([Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')))
$baseUrl = "jdbc:mysql://127.0.0.1:$Port/"
$suffix = '?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true'
$values = @{
    MAVEN_OPTS = ($env:MAVEN_OPTS + ' -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8')
    BIRTHDAY_INTEGRATION_TEST = 'true'; BIRTHDAY_TEST_DB_PORT = "$Port"; BIRTHDAY_TEST_HTTP_PORT = "$HttpPort"
    BIRTHDAY_TEST_FILES = [IO.Path]::GetFullPath($files)
    DB_HOST = '127.0.0.1'; DB_PORT = "$Port"; DB_NAME = 'nw_birthday_test'
    DB_USER = 'birthday_test_app'; DB_PASSWORD = 'birthday-test-app-only'
    BIRTHDAY_DATA_JDBC_URL = $baseUrl + 'nw_birthday_test' + $suffix
    BIRTHDAY_VAULT_JDBC_URL = $baseUrl + 'nw_birthday_vault' + $suffix
    BIRTHDAY_VAULT_DB_USER = 'birthday_test_vault'; BIRTHDAY_VAULT_DB_PASSWORD = 'birthday-test-vault-only'
    BIRTHDAY_KEYSTORE_PATH = [IO.Path]::GetFullPath("$files/keys/birthday.p12")
    BIRTHDAY_KEYSTORE_PASSWORD_FILE = [IO.Path]::GetFullPath("$files/secrets/password")
    BIRTHDAY_E2E = 'true'; E2E_BASE_URL = "http://127.0.0.1:$HttpPort"; PLAYWRIGHT_HTML_OPEN = 'never'
}
$saved = @{}
$savedEncoding = [Console]::OutputEncoding
$savedPipelineEncoding = $OutputEncoding
foreach ($name in $values.Keys) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process'); [Environment]::SetEnvironmentVariable($name, $values[$name], 'Process') }
Push-Location $appRoot
function Compose { & docker compose -p $project -f $compose @args; if ($LASTEXITCODE -ne 0) { throw 'Disposable birthday environment command failed.' } }
function VerifyBackup {
    $classPath = (Join-Path $appRoot 'target/app/WEB-INF/classes') + [IO.Path]::PathSeparator + (Join-Path $appRoot 'target/app/WEB-INF/lib/*')
    & java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp $classPath jp.nw.security.BirthdayAdmin verify
    if ($LASTEXITCODE -ne 0) { throw 'Birthday verification failed.' }
}
try {
    [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
    $OutputEncoding = [Console]::OutputEncoding
    & (Join-Path $PSScriptRoot 'bootstrap-security.ps1') -Source $Source -Maven $Maven -Repository $Repository
    Compose up -d --wait --wait-timeout 240 mysql
    Compose run --rm app-flyway
    Compose run --rm vault-flyway
    & $Maven "-Dmaven.repo.local=$Repository" --batch-mode -Pbirthday-it verify
    if ($LASTEXITCODE -ne 0) { throw 'Birthday integration tests failed.' }
    $container = (& docker compose -p $project -f $compose ps -q mysql).Trim()
    $dump = & docker exec -e MYSQL_PWD=birthday-test-root-only $container mysqldump --default-character-set=utf8mb4 --single-transaction --set-gtid-purged=OFF --databases nw_birthday_test nw_birthday_vault
    if ($LASTEXITCODE -ne 0) { throw 'Test database backup failed.' }
    [IO.File]::WriteAllLines("$files/backup/databases.sql", [string[]]$dump, [Text.UTF8Encoding]::new($false))
    Copy-Item -LiteralPath $env:BIRTHDAY_KEYSTORE_PATH -Destination "$files/backup/birthday.p12"
    Copy-Item -LiteralPath $env:BIRTHDAY_KEYSTORE_PASSWORD_FILE -Destination "$files/backup/password"
    Compose restart mysql
    Compose up -d --wait --wait-timeout 120 mysql
    VerifyBackup
    $restored = ([string]::Join("`n", $dump)).Replace('`nw_birthday_test`', '`nw_birthday_restore`').Replace('`nw_birthday_vault`', '`nw_vault_restore`')
    $restored += "`nGRANT SELECT,INSERT,UPDATE,DELETE ON nw_birthday_restore.* TO 'birthday_test_app'@'%';`nGRANT SELECT,INSERT,UPDATE,DELETE ON nw_vault_restore.* TO 'birthday_test_vault'@'%';`n"
    [IO.File]::WriteAllText("$files/backup/restore.sql", $restored, [Text.UTF8Encoding]::new($false))
    & docker cp "$files/backup/restore.sql" "${container}:/tmp/birthday-restore.sql"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot stage test backup for restore.' }
    & docker exec -e MYSQL_PWD=birthday-test-root-only $container mysql --default-character-set=utf8mb4 -e 'source /tmp/birthday-restore.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Test backup restore failed.' }
    $env:BIRTHDAY_DATA_JDBC_URL = $baseUrl + 'nw_birthday_restore' + $suffix
    $env:BIRTHDAY_VAULT_JDBC_URL = $baseUrl + 'nw_vault_restore' + $suffix
    $env:DB_NAME = 'nw_birthday_restore'
    $env:BIRTHDAY_KEYSTORE_PATH = [IO.Path]::GetFullPath("$files/backup/birthday.p12")
    $env:BIRTHDAY_KEYSTORE_PASSWORD_FILE = [IO.Path]::GetFullPath("$files/backup/password")
    VerifyBackup
    foreach ($name in @('BIRTHDAY_DATA_JDBC_URL','BIRTHDAY_VAULT_JDBC_URL','BIRTHDAY_KEYSTORE_PATH','BIRTHDAY_KEYSTORE_PASSWORD_FILE','DB_NAME')) { [Environment]::SetEnvironmentVariable($name, $values[$name], 'Process') }
    if (-not $SkipBrowser) {
        Compose up -d tomcat
        $ready = $false
        for ($attempt = 0; $attempt -lt 60; $attempt++) {
            try { $response = Invoke-WebRequest -Uri "$env:E2E_BASE_URL/Login" -UseBasicParsing; if ($response.StatusCode -eq 200) { $ready = $true; break } } catch { }
            Start-Sleep -Seconds 2
        }
        if (-not $ready) { Compose logs tomcat; throw 'Test Tomcat did not start.' }
        & npx playwright test tests/birthday.spec.ts --project=chromium --workers=1 --reporter=line
        if ($LASTEXITCODE -ne 0) { throw 'Birthday browser tests failed.' }
    }
    Write-Output 'PASS: registration, authorization, migration retry, missing keys, MySQL restart and backup restore.'
} finally {
    # The project name is generated above; only this test environment is removed.
    & docker compose -p $project -f $compose down -v
    foreach ($name in $values.Keys) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
    [Console]::OutputEncoding = $savedEncoding
    $OutputEncoding = $savedPipelineEncoding
    Pop-Location
}
