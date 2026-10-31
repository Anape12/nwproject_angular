# 初期セットアップ

1. 資産生成
    - .\mvnw clean install
1. コンテナ構築
    - docker compose up -d --build

# コンテナ削除 → 再生成

    * docker compose -f .devcontainer/docker-compose.yml down -v
    * docker compose -f .devcontainer/docker-compose.yml up -d --build

# コンテナ再構築（データ保持）

    * docker compose --env-file .env -f .devcontainer/docker-compose.yml down
    * docker compose --env-file .env -f .devcontainer/docker-compose.yml up -d --build

# Java 資産の反映

    * docker restart my-tomcat

# SQL 反映（Flyway）

DB を削除せずに未適用の SQL を反映する手順は [DB マイグレーションの適用](docs/database-migrations.md) を参照してください。プロジェクト直下から `.env` と Compose ファイルを指定して実行します。

# Playwrite のテスト実行

    * npx playwright test

# Prettier によるコード整形

初回のみ、プロジェクトの依存関係をインストールします。

```bash
npm install
```

現在 Prettier に対応している JSP を整形します。

```bash
npm run format:jsp
```

対応している JSP、JavaScript、CSS、Markdown、JSON、YAML を一括整形します。

```bash
npm run format
```

ファイルを変更せず、整形が必要か確認します。

```bash
npm run format:check
```

任意のファイルだけを整形する場合は、次のように実行します。

```bash
npx prettier --write "src/main/webapp/WEB-INF/jsp/security/auditLog.jsp"
```

JSP の整形には`prettier-plugin-jsp`を使用します。プラグインが解析できない旧形式の JSP は`.prettierignore`で除外しているため、構文を移行してから自動整形の対象へ追加してください。

# Prettier の適用

-   npm run format:jsp

# Flyway 実行

適用状況の確認、実行、失敗時の注意点は [DB マイグレーションの適用](docs/database-migrations.md) にまとめています。通常のマイグレーションで `docker compose down -v` は実行しません。

# 修正後の資産適用

1. cd nwproject_docker
1. mvnw package
1. ブラウザ super reload

# VSCode キャッシュ先

    * %APPDATA%\Code\User\workspaceStorage

# My-AI との接続

cd C:\Users\tmng1\workspace\nwproject_docker

docker compose `  --env-file .env`
-f .devcontainer/docker-compose.yml `  -f .devcontainer/docker-compose.ai.yml`
up -d --build --force-recreate

-   war 再作成
    docker compose `  --env-file .env`
    -f .devcontainer/docker-compose.yml `  -f .devcontainer/docker-compose.ai.yml`
    run --rm -w /workspaces app ./mvnw clean package

-   tomcat 再構築
    docker compose `  --env-file .env`
    -f .devcontainer/docker-compose.yml `  -f .devcontainer/docker-compose.ai.yml`
    up -d --build --force-recreate

# Qiita Guide（記事の読み上げ）との連携

業務メニューの「Qiita 記事を聴く」から、別プロジェクトの`AI_Support`（Qiita Guide）を新しいタブで開きます。NW Project と Qiita Guide はそれぞれ起動する必要があります。

ローカルでは`AI_Support`を`nwproject_docker`と同階層に配置し、`AI_Support/.env`に OpenAI API キーを設定してください。Qiita Guide 用の Compose ファイルを追加すれば、Node.js を別ターミナルで起動する必要はありません。PowerShell で`nwproject_docker`直下から実行します。

```powershell
docker compose --env-file .env -f .devcontainer/docker-compose.yml -f .devcontainer/docker-compose.ai.yml -f .devcontainer/docker-compose.qiita.yml up -d --build
```

Qiita Guide だけを再ビルドして起動する場合は次のコマンドを使います。

```powershell
docker compose --env-file .env -f .devcontainer/docker-compose.yml -f .devcontainer/docker-compose.ai.yml -f .devcontainer/docker-compose.qiita.yml up -d --build qiita-guide
```

`http://localhost:3000/api/status`で起動状態を確認できます。NW Project を`localhost`で開いている場合、`QIITA_GUIDE_URL`を省略すると`http://localhost:3000/`を使用します。Qiita Guide のポートはホストのループバックアドレスにだけ公開します。

別端末や Cloud で利用する場合は、Qiita Guide をアクセス可能な HTTPS の URL で公開し、NW Project の`.env`に接続先を設定します。

```text
QIITA_GUIDE_URL=https://reader.example.com/
```

設定変更後は Tomcat コンテナを再作成して環境変数を反映します。Cloud では`localhost`を接続先に指定しないでください。上記 Compose 設定はローカル用で、Cloud でブラウザーから使うには HTTPS のリバースプロキシと Qiita Guide 側のアクセス制御が別途必要です。NW Project のログイン制御はメニューの起動口にのみ適用されます。

# 技術スタック

-   アプリケーション：Java 21、Servlet 4.0／JSP／JSTL、JavaScript、CSS。Maven で WAR をビルドし、Apache Tomcat 9 で実行。
-   主なライブラリ：MySQL Connector/J、Jackson、Lombok、Spring Security の BCrypt パスワードエンコーダー。
-   データベース：MySQL 8.0、Flyway 11 による SQL マイグレーション。
-   ローカル実行環境：Docker Compose、Nginx によるリバースプロキシと TLS 終端。
-   任意の AI 連携：別プロジェクトの My-AiCreate と、その API 経由で利用する Ollama。
-   任意の記事読み上げ連携：Node.js 22 でコンテナ化した別プロジェクトの Qiita Guide（AI_Support）。
-   テスト・整形：JUnit 5、Playwright による E2E テスト、Prettier と`prettier-plugin-jsp`。
-   CI・デプロイ：GitHub Actions。Google Compute Engine は任意のデプロイ先として定義（ローカル開発には不要）。
