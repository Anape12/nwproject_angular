# 生年月日の暗号化保存

`jpa-security` を使って、生年月日の登録・表示・変更・既存データ移行を実装しています。氏名や他の情報の保存方式は変更していません。

## 構成とアクセス権

-   業務 DB: `users_info.birthday_secret_id` と `payloads` の暗号文。
-   対応表 DB: 別の MySQL コンテナー `birthday-vault` の `mappings`。
-   鍵: ホストの `.local/birthday/keys/birthday.p12`。パスワードは別ファイル `.local/birthday/secrets/password`。

鍵・秘密ファイルと生成したライブラリは Git・Docker イメージに含めません。鍵ファイルへの書込権限は、ロックファイルの利用にも必要です。実行ユーザー以外のアクセスを制限し、秘密ファイルの内容をログへ出さないでください。

本人は自分の生年月日を操作できます。管理者は一般・管理者アカウント、システム管理者はシステム管理者も操作できます。AI・無効化された操作元・削除済みユーザー・不明な権限は拒否します。操作のたびに DB の最新の役割と状態を確認します。

認証された暗号文の中に `nwproject:users_info:<内部ID>:birthday:v1` と日付を格納し、復号後に所有関係を照合します。ログイン ID を変更しても内部 ID は維持されます。参照 ID を他ユーザーへ差し替えても、その人の生年月日は返しません。

## 1. 依存ライブラリを準備する

隣に `jpa-security` がある場合、Windows では次を実行します。

```powershell
.\scripts\bootstrap-security.ps1
.\mvnw.cmd clean install
```

別の場所にある場合は `-Source C:\path\to\jpa-security`、または環境変数 `JPA_SECURITY_SOURCE` を指定します。Linux では `bash scripts/bootstrap-security.sh` を使います。両スクリプトとも、認証して取得済みのローカルチェックアウトだけを使います。ソースがなければ説明付きで停止し、匿名での取得や公開への切替は行いません。

生成先は `.build-deps/maven` のプロジェクト用 Maven リポジトリーです。Docker ビルド前にもこの準備が必要です。開発用 JAR を作り直したときは bootstrap を再実行してください。Maven Central への公開やユーザー固有の Maven キャッシュへ依存しません。

### private リポジトリの取得権限

`Anape12/jpa-security` は private で運用します。開発端末では、アクセス権のある SSH 鍵または Git Credential Manager 等を使って取得します。トークンを clone URL やスクリプトへ埋め込まず、このチャットへ貼り付ける必要もありません。

CI では `nwproject_angular` の **Settings → Secrets and variables → Actions** に `JPA_SECURITY_READ_TOKEN` を登録します。fine-grained PAT を使う場合、対象を `Anape12/jpa-security` のみに限定し、Repository permissions の **Contents: Read-only** を付与します。書込権限は不要です。有効期限を設けて更新してください。

通常の `GITHUB_TOKEN` は呼び出し元リポジトリ用なので、別の private リポジトリの取得にはこの専用の認証情報が必要です。両ビルドワークフローは、固定したコミットを `actions/checkout` で取得し、`persist-credentials: false` で Git 設定への認証情報の残留を避けます。未設定なら明示的に失敗し、private 依存へアクセスするジョブは外部 fork からの PR では実行しません。

サーバーでも、事前に読み取り専用の deploy key 等で `jpa-security` を取得し、隣のフォルダーへ配置するか `JPA_SECURITY_SOURCE` を設定します。`bootstrap-security.sh` はサーバー上でも自動 clone やブランチ変更をしません。

WAR とコンテナーイメージには実行に必要なライブラリのバイトコードが含まれます。`nwproject_docker` が public でもそれを無制限に配布しないよう、WAR の Actions artifact アップロードは private リポジトリ時だけに制限し、private ライブラリが入る Maven キャッシュの Actions 保存を無効にしています。配備用イメージや WAR の配布先にもアクセス制限を設けてください。

参考: [actions/checkout の private リポジトリ取得](https://github.com/actions/checkout#checkout-multiple-repos-private)、[GitHub Actions のキャッシュとアクセス範囲](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching)。

### ライブラリが見つからない場合

`dev.kobun:jpa-security:0.1.0-SNAPSHOT` は Maven Central では公開していません。初回チェックアウト後は、上の bootstrap を先に実行して `.build-deps/maven` を用意してください。`clean` はこの依存ライブラリを作成しません。

ローカルの `security-local` リポジトリーだけは SNAPSHOT の `updatePolicy=always` とし、以前の取得失敗がキャッシュされていても、次回起動時に再確認します。既存環境でまだキャッシュのエラーが出る場合は、次で更新を強制できます。Maven キャッシュ全体を削除する必要はありません。

```powershell
.\scripts\bootstrap-security.ps1
.\mvnw.cmd -U clean install
```

参考: [Maven のリポジトリー更新ポリシー](https://maven.apache.org/settings.html#Repositories)。

## 2. スキーマと秘密情報を準備する

通常の Flyway へ `V26__protect_user_birthdays.sql` を追加しています。既存の移行 SQL は変更していません。V26 は参照列・暗号文テーブル・メッセージを追加するだけで、既存の生年月日はこの段階では消しません。

`.env.template` を参考に、別々の強い `BIRTHDAY_VAULT_DB_PASSWORD` と `BIRTHDAY_VAULT_ROOT_PASSWORD` を設定します。対応表 DB の通常接続はアプリ用ユーザー、Flyway だけ管理用ユーザーです。Compose overlay は `.devcontainer/docker-compose.birthday.yml` です。

ホストに `.local/birthday/keys` と `.local/birthday/secrets` を作り、十分に長いランダムなパスワードを `secrets/password` へ保存します。パスワードはソースやコマンド引数に埋め込みません。

キー作成・移行・検証は、Tomcat を停止した状態の管理操作です。同じ鍵ファイルを複数プロセスで開けないため、稼働中に次のコマンドを実行しないでください。

```bash
docker compose --env-file .env -f .devcontainer/docker-compose.yml -f .devcontainer/docker-compose.birthday.yml stop tomcat
docker compose --env-file .env -f .devcontainer/docker-compose.yml -f .devcontainer/docker-compose.birthday.yml run --rm --no-deps --entrypoint java tomcat -cp '/usr/local/tomcat/webapps/ROOT/WEB-INF/classes:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/*' jp.nw.security.BirthdayAdmin init-keys
```

`init-keys` は初回専用です。既存の鍵を上書きしません。通常起動では `open` のみを使い、鍵がない・パスワードが誤っている・壊れている場合に鍵を自動生成しません。

## 3. 適用と移行

通常の Flyway と `birthday-flyway` を実行し、両 DB のスキーマを適用します。その後、上の管理コマンドの末尾を `migrate` に変えて実行します。

`migrate` は 100 件ずつ候補を取得し、行をロックして 1 件単位で処理します。暗号化保存し、所有関係を含めて復号確認ができた後、同じ業務 DB トランザクション内で参照を設定し、平文を NULL にします。監査には操作名と対象ユーザーだけを記録し、生年月日は記録しません。

失敗した行はロールバックされ、既に確定した行は残ります。再実行は未移行の行だけを対象とします。末尾 `verify` で、平文が残っていないことと全参照の復号を確認できます。移行前の平文へ自動的に読み戻す処理はありません。未移行の表示は 409、設定・鍵・暗号化の障害は 503 になります。

暗号化と業務 DB の更新は分散トランザクションではありません。参照更新に失敗すると、参照されない暗号化レコードが残る場合があります。応答が失われたコミットを考慮して、例外時に暗号化レコードを即時削除しません。不要行の回収は別の運用段階です。

全件確認後、管理用 DB 接続で `scripts/sql/birthday-finalize.sql` を適用すると、以後の平文保存を DB でも拒否できます。既存行の移行前にこの制約を適用しないでください。

`scripts/deploy.sh` は鍵ファイルが配置されている場合にだけ overlay を有効にし、対応表 DB 起動・Flyway・Tomcat 停止・オフライン移行・Tomcat 再作成の順に処理します。鍵未設定でもログインなど既存機能は起動しますが、生年月日を必要とする新規登録・確認・変更は利用できません。平文への自動切替はありません。

## 4. 画面

ヘッダーの「生年月日」から本人の確認・変更ができます。管理画面の各対象ユーザーにもリンクがあります。POST は CSRF トークン必須で、表示と応答には `Cache-Control: no-store` を付けます。

## 5. 再起動・バックアップ・復元

復旧には、整合する業務 DB、対応表 DB、鍵ファイル、鍵パスワードの 4 点が必要です。PKCS12 ファイルだけでも、DB だけでも復旧できません。旧鍵はバックアップの保管期限を確認するまで廃止しません。

1. 書込と管理バッチを停止し、Tomcat を停止して鍵ファイルのロックを解放します。
2. その状態で両 DB をバックアップし、対応する PKCS12 と秘密ファイルを別の安全な保管先へ退避します。
3. 復旧用の別 DB へリストアし、対応する鍵とパスワードを配置します。
4. 復旧用接続先を指定して `BirthdayAdmin verify` を実行します。
5. 確認後に Tomcat を再開します。

この文書だけで運用バックアップを自動実行する仕組みはありません。外部 KMS、複数インスタンス、旧暗号文の回収、バックアップの保管・廃止は今後の運用拡張です。別コンテナーでも同一 Docker ホストの管理権限からは完全に隔離されません。

## 自動検証

Docker を起動し、次を実行します。

```powershell
.\scripts\test-birthday.ps1
```

専用プロジェクト名の MySQL と Tomcat を作り、既存の稼働 DB へは接続しません。ポート 13326 と 18086 が使用中なら `-Port` / `-HttpPort` で変更できます。

通常テストに加えて、実際の全 26 件の業務 Flyway、対応表 Flyway、登録・取得・更新、本人/管理者/別ユーザーの認可、DB 参照の差し替え、移行途中失敗と再開、鍵欠落時の拒否を検証します。その後に MySQL を再起動し、mysqldump と鍵のコピーから別 DB へ復元して、別の Java プロセスで検証します。最後にブラウザーで確認・変更・アクセス拒否・CSRF 拒否を確認します。

終了時はこの検証用コンテナーとボリュームだけを削除します。生成した検証用ファイルは `.local/birthday-test` 内にあり、実データや運用鍵は使いません。CI の `birthday-encryption.yml` も同じ手順を実行します。
