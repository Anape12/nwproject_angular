param([string]$Source, [string]$Maven, [string]$Repository)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $Source) { $Source = $env:JPA_SECURITY_SOURCE }
if (-not $Source) {
    $sibling = Join-Path $projectRoot '../jpa-security'
    $ciCheckout = Join-Path $projectRoot '.build-deps/jpa-security-source'
    $Source = if (Test-Path -LiteralPath (Join-Path $sibling 'security-core/pom.xml')) { $sibling } else { $ciCheckout }
}
if (-not (Test-Path -LiteralPath (Join-Path $Source 'security-core/pom.xml'))) {
    throw 'Private jpa-security checkout is required. Use authorized Git credentials, then set -Source or JPA_SECURITY_SOURCE. See docs/birthday-encryption.md.'
}
if (-not $Maven) { $Maven = Join-Path $projectRoot 'mvnw.cmd' }
if (-not $Repository) { $Repository = Join-Path $projectRoot '.build-deps/cache' }
$Source = [IO.Path]::GetFullPath($Source)
$Repository = [IO.Path]::GetFullPath($Repository)
& $Maven '-f' (Join-Path $Source 'pom.xml') "-Dmaven.repo.local=$Repository" '-pl' 'security-core' '-am' '-DskipTests' '--batch-mode' 'install'
if ($LASTEXITCODE -ne 0) { throw 'Security library build failed.' }
foreach ($artifact in @('jpa-security-parent', 'jpa-security')) {
    $relative = "dev/kobun/$artifact/0.1.0-SNAPSHOT"
    $destination = Join-Path $projectRoot ".build-deps/maven/$relative"
    New-Item -ItemType Directory -Force $destination | Out-Null
    Copy-Item -LiteralPath (Join-Path $Repository "$relative/$artifact-0.1.0-SNAPSHOT.pom") -Destination $destination
    if ($artifact -eq 'jpa-security') {
        Copy-Item -LiteralPath (Join-Path $Repository "$relative/$artifact-0.1.0-SNAPSHOT.jar") -Destination $destination
    }
}
Write-Output 'Prepared the local Maven repository for the web application and Docker build.'
