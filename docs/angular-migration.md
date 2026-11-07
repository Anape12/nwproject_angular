# JSP から Angular への移行

1. ログイン画面と認証失敗画面は静的 HTML から Angular を起動する。Angular は `/Login` にフォームを送信し、Java が認証とセッション管理を行う。
2. メニュー選択、チャットメニュー、業務メニュー、共通ヘッダーを Angular に移す。画面間の移動は既存のリンクとフォームを使い、タブごとのログイン状態も既存のスクリプトで引き継ぐ。
3. 業務機能ごとに画面と必要な API を移す。閲覧中心の画面から始め、更新・承認操作、チャットや添付ファイルを扱う画面へ進める。画面ごとに権限、セッション切れ、入力エラーを確認する。
4. 全画面の移行後に JSP の起動用テンプレート、共通部品、参照されなくなった静的ファイルを整理する。

第3段階では、監査ログ検索を先行移行した。`/AuditLog` は Angular を起動し、`/AuditLog?format=json` は同じ閲覧権限と検索条件で画面表示用のデータを返す。

業務ダッシュボードも Angular に移行した。`/Portal?format=json` で集計と横断検索結果を取得する。通知の既読化と申請の取下げは、従来の `POST /Portal` で処理する。

ユーザー検索は従来の `POST /UserSearch` を使い、検索結果の表示に必要な項目を Angular に渡す。ユーザー情報の編集は既存の画面へ遷移する。

パスワード変更、生年月日の確認・変更、新規スレッド、Qiita Guide の接続先未設定画面を Angular に移行した。フォームは従来の Java コントローラーに送信する。

スレッド一覧、チャット一覧、個別チャット作成と、登録完了・エラーなどの結果画面も移行した。設計リバースは既存の読み取り専用 API を使い、一覧、検索、詳細、関係図、JSON 保存を Angular で提供する。

スレッド詳細を Angular に移し、コメント投稿、完了・再開、AI 返信待ちの再読み込みは既存の動作を引き継ぐ。

チャット本体とメンバー管理を Angular に移した。メッセージの送信・定期取得、添付ファイル、招待は既存のエンドポイントを使う。

新規ユーザー登録とアカウント・セキュリティ管理を Angular に移した。登録、基本情報・権限の更新、アカウントの有効化・無効化、ロック解除、パスワード再設定は既存のコントローラーが処理する。

AI 住人管理を Angular に移し、新規登録・設定変更、稼働状態、直近の応答状況を表示する。保存と権限の判定には既存のコントローラーを使う。

報告書作成と承認管理を Angular に移した。下書き保存・提出・削除、添付、承認・差戻し、承認段階・代理承認の設定は従来のコントローラーに送信する。

業務メニューの予定カレンダーを Angular に移した。月表示、予定の追加・編集・削除、参加回答、添付、iCalendar の入出力には既存のコントローラーを使う。

勤怠表を Angular に移した。打刻、実績保存、日次・月次申請、締め処理と CSV 出力は既存のコントローラーを使う。

業務メニューから開く AX3・AX4 の外部アプリ画面とファイル読込画面を Angular に移した。ファイルは既存のアップロード処理に送信する。

移行対象の画面は静的 HTML で Angular を起動し、表示データは API から取得する。

有効な参照がなくなった旧予定入力、旧日記、共通フッター、旧ログアウト、旧登録確認・完了、旧登録用ログイン失敗の JSP を削除した。

## ビルド

`/Login` は `WEB-INF/angular/auth/login.html` から Angular を起動する。認証結果とセッション切れの表示メッセージは `/Login?format=json` から取得する。

`/MenuSelect`、`/CommunicationMenu`、`/BusinessMenu` は静的 HTML から起動する。利用者名とタブごとのログイン情報は各 URL の `format=json` で取得する。業務メニューは権限別の表示設定と日付も同じ API から取得し、共通ヘッダーを Angular から直接表示する。

`/Portal`、`/AuditLog`、`/ReverseEngineering` も静的 HTML から起動する。共通ヘッダーの情報は `/MenuSelect?format=json`、各画面の表示データはそれぞれの既存 API から取得する。

`/ThreadController`、`/ThreadCreate`、`/ChatChanelList` も静的 HTML から起動する。スレッド一覧とチャット一覧はそれぞれの URL の `format=json` から取得する。新規スレッドの送信先と作成後の遷移先は従来どおり。

`/ThreadDetailController` も静的 HTML から起動し、本文、コメント、操作権限、フォーム用トークン、AI 返信待ちの状態を同じ URL の `format=json` から取得する。

`/ChatChanelRoom` と `/ChatMemberManage` は静的 HTML から起動する。初期メッセージ、添付情報、メンバー情報、操作に必要なトークンは各 URL の `format=json` から取得し、部屋の閲覧権限とメンバー管理権限は HTML と API の両方に適用する。

`/ChatRoomCreate` と Qiita Guide の接続先未設定画面も静的 HTML から起動する。個別チャットの候補と作成結果は `/ChatRoomCreate?format=json` で取得する。

`/WorkManagement` と `/OpenCalender` も静的 HTML から起動する。月表示、編集対象、操作権限、フォーム用トークンは各 URL の `format=json` から取得する。

`/DairyWrite` と `/ReportApproval` も静的 HTML から起動する。報告書と添付の情報、申請一覧と承認履歴、操作メッセージ、フォーム用トークンは各 URL の `format=json` から取得する。

`/UserSecurityAdmin` と `/AiCharacterAdmin` も静的 HTML から起動する。アカウント一覧、AI 住人の設定と応答状況、操作メッセージ、フォーム用トークンは各 URL の `format=json` で取得する。

`/ChangePassword` と `/UserBirthday` も静的 HTML から起動する。パスワード変更の初期化は同じ URL の API を使い、初回変更が必須のセッションでもメニューへのアクセスを経由しない。生年月日の読込と更新には既存の権限判定と監査処理を使う。

`/UserInsert` と `/UserSearch` も静的 HTML から起動する。登録時の検証エラーと検索結果は JSON で受け取る。

AX3・AX4 の外部アプリ画面とファイル読込画面も静的 HTML から起動する。選択アプリ名は `/SelectApp?format=json` で取得し、ファイル送信先は従来どおり。

現行画面から参照されていなかった旧ルート `/OpenShedule`、`/WriteShedule`、`/UserView`、`/EditUserView`、`/UserInsertResult` を廃止した。関連する旧予定、旧ユーザー一覧、結果・エラー画面の JSP を削除した。

ユーザー検索結果の旧編集リンクと `/UpdateUserInfo` を廃止した。旧編集 URL にアクセスした場合は Angular のユーザー検索画面を表示する。旧編集画面と共通部品を削除し、アプリケーションの JSP および JSP 断片はすべて削除済み。

検証用の Tomcat と新規 MySQL で Flyway 27 件を適用し、Edge でログイン失敗・成功、メニュー切替、業務ダッシュボード、勤怠表、予定カレンダー、報告書作成、スレッド一覧の表示を確認した。

同じ構成の一時環境で、報告書の下書き保存・再表示・提出、勤怠の保存・再表示、終日予定の登録・再表示、スレッドの作成とコメント投稿・再表示も確認した。検証環境は終了後に破棄した。

別アカウントでの報告書承認と、提出者・承認者双方での承認済み表示を確認した。承認権限のないユーザーには画面と API の両方が HTTP 403 を返した。個別チャットは作成・送信後に再読み込みし、相手側の画面にも同じメッセージが表示されることを確認した。

Angular のソースは `frontend` に集約している。`frontend/src/<機能>` にコンポーネントとテンプレート、`frontend/pages/<機能>` に画面起動用 HTML を置く。共通処理は `frontend/src/shared`、起動処理は `frontend/src/main.ts` に置く。

通常の `mvnw package` で、Maven が Node.js を取得し、Angular をビルドして `target/app.war` に同梱する。画面起動用 HTML は WAR 内の `WEB-INF/angular/<機能>` に、ビルド済み JavaScript は `js/angular` に配置する。Windows では `.\mvnw.cmd package` を実行する。

Angular の変更だけを確認する場合は、Node.js 22.12 以降の 22 系または 24 系で `npm ci` と `npm run build:frontend` を実行する。成果物は `target/angular/browser` に出力される。
