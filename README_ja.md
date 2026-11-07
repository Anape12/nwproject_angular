# NW Project Angular

## Windows で画面を起動する

JDK 21 と Docker Desktop（Linux コンテナ）を用意し、このリポジトリの直下で PowerShell を開きます。初回ビルドには、アクセス権のある `jpa-security` のチェックアウトも必要です。[依存ライブラリの準備](docs/birthday-encryption.md)

以下では `-p nwproject-angular` で DB ボリュームを分け、`.env` でコンテナ名と公開ポートを分けます。元の `nwproject_docker` は起動したままで構いません。

1. 初回だけ `.env` を作成し、`DB_PASSWORD` と `MYSQL_ROOT_PASSWORD` を設定します。既存の `.env` がある場合はそのファイルを使います。アプリ用の `DB_USER` は `nwproject_app` を使います。MySQL の初期化では `DB_USER=root` を指定すると起動に失敗します。

    ```powershell
    Copy-Item .env.template .env
    notepad .env
    ```

    既存の `.env` を使う場合は、次の設定を追加します。

    ```dotenv
    NW_DB_CONTAINER=nwproject-angular-db
    NW_TOMCAT_CONTAINER=nwproject-angular-tomcat
    NW_DB_HOST_PORT=3308
    NW_HTTP_PORT=8081
    NW_DEBUG_PORT=5006
    ```

2. 依存ライブラリを準備し、アプリをビルドします。Maven が Node.js の取得、Angular のビルド、WAR への同梱を行います。

    ```powershell
    .\scripts\bootstrap-security.ps1
    .\mvnw.cmd clean package
    ```

3. Docker Desktop を起動した状態で、DB の準備と Tomcat の起動を順番に実行します。Flyway が成功したことを確認してから Tomcat を起動します。

    ```powershell
    docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml up -d db
    docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml run --rm flyway
    docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml up -d --build tomcat
    ```

4. ブラウザーでログイン画面を開きます。[http://localhost:8081/Login](http://localhost:8081/Login)

ログインやメニューの表示は、上記の Tomcat と MySQL で確認できます。新規ユーザー登録と生年月日の確認・変更には、暗号化用の鍵と対応表 DB が必要です。準備したら、起動時に `-f .devcontainer/docker-compose.birthday.yml` を追加してください。[生年月日の暗号化設定](docs/birthday-encryption.md)

AI 住人と Qiita Guide を使う場合は、それぞれの追加設定が必要です。

## 変更を反映する

Angular コンポーネントとそのテンプレートは `frontend/src/<機能>`、画面起動用 HTML は `frontend/pages/<機能>` で編集します。Maven は画面起動用 HTML を WAR 内の `WEB-INF/angular/<機能>` に配置します。

Tomcat は `target/app` を参照します。Angular または Java を変更したら、ビルドして Tomcat を再起動します。

```powershell
.\mvnw.cmd package
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml restart tomcat
```

画面が開かない場合は、次のコマンドで起動ログを確認します。

```powershell
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml logs --tail 100 tomcat
```

Angular のビルド方法と旧画面の扱いは、[Angular 移行計画](docs/angular-migration.md)を参照してください。

# SQL 反映（Flyway）

DB を削除せずに未適用の SQL を反映する手順は [DB マイグレーションの適用](docs/database-migrations.md) を参照してください。この README の手順で起動した環境では、同じ DB を使うために各 Compose コマンドにも `-p nwproject-angular` を指定します。

# Playwrite のテスト実行

    * npx playwright test

# Prettier によるコード整形

初回のみ、プロジェクトの依存関係をインストールします。

```bash
npm install
```

対応している JavaScript、CSS、Markdown、JSON、YAML を一括整形します。

```bash
npm run format
```

ファイルを変更せず、整形が必要か確認します。

```bash
npm run format:check
```

任意のファイルだけを整形する場合は、次のように実行します。

```bash
npx prettier --write "frontend/src/shared/login-context.ts"
```

# Flyway 実行

適用状況の確認、実行、失敗時の注意点は [DB マイグレーションの適用](docs/database-migrations.md) にまとめています。通常のマイグレーションで `docker compose down -v` は実行しません。

# VSCode キャッシュ先

    * %APPDATA%\Code\User\workspaceStorage

# My-AI との接続

先に通常の起動手順を実行し、[AI 住人のローカル設定](docs/ai-character-local.md)を準備します。このリポジトリの直下で AI 用の Compose 定義を追加します。

```powershell
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml -f .devcontainer/docker-compose.ai.yml up -d --build tomcat
```

# Qiita Guide（記事の読み上げ）との連携

業務メニューの「Qiita 記事を聴く」から、別プロジェクトの`AI_Support`（Qiita Guide）を新しいタブで開きます。NW Project と Qiita Guide はそれぞれ起動する必要があります。

ローカルでは `AI_Support` をこのリポジトリと同階層に配置し、`AI_Support/.env` に OpenAI API キーを設定します。PowerShell でこのリポジトリの直下から実行します。

```powershell
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml -f .devcontainer/docker-compose.ai.yml -f .devcontainer/docker-compose.qiita.yml up -d --build tomcat qiita-guide
```

Qiita Guide だけを再ビルドして起動する場合は次のコマンドを使います。

```powershell
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml -f .devcontainer/docker-compose.ai.yml -f .devcontainer/docker-compose.qiita.yml up -d --build qiita-guide
```

`http://localhost:3000/api/status`で起動状態を確認できます。NW Project を`localhost`で開いている場合、`QIITA_GUIDE_URL`を省略すると`http://localhost:3000/`を使用します。Qiita Guide のポートはホストのループバックアドレスにだけ公開します。

別端末や Cloud で利用する場合は、Qiita Guide をアクセス可能な HTTPS の URL で公開し、NW Project の`.env`に接続先を設定します。

```text
QIITA_GUIDE_URL=https://reader.example.com/
```

設定変更後は Tomcat コンテナを再作成して環境変数を反映します。Cloud では`localhost`を接続先に指定しないでください。上記 Compose 設定はローカル用で、Cloud でブラウザーから使うには HTTPS のリバースプロキシと Qiita Guide 側のアクセス制御が別途必要です。NW Project のログイン制御はメニューの起動口にのみ適用されます。

# 技術スタック

-   アプリケーション：Angular 21、TypeScript、Java 21、Servlet 4.0。Maven で WAR をビルドし、Apache Tomcat 9 で実行。
-   主なライブラリ：MySQL Connector/J、Jackson、Lombok、Spring Security の BCrypt パスワードエンコーダー。
-   データベース：MySQL 8.0、Flyway 11 による SQL マイグレーション。
-   ローカル実行環境：Docker Compose、Nginx によるリバースプロキシと TLS 終端。
-   任意の AI 連携：別プロジェクトの My-AiCreate と、その API 経由で利用する Ollama。
-   任意の記事読み上げ連携：Node.js 22 でコンテナ化した別プロジェクトの Qiita Guide（AI_Support）。
-   テスト・整形：JUnit 5、Playwright による E2E テスト、Prettier。
-   CI：GitHub Actions で Angular と Java のビルド、WAR の内容確認、Flyway の適用、ブラウザーテストを実行します。生年月日の暗号化は別のワークフローで検証します。
