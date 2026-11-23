# 更新確認ダイアログ

`CustomDialogComponent` は、登録・保存・削除・申請・承認などの変更を確定する前に確認する共通部品です。
ログイン・検索・一覧表示には使用しません。

フォームでは `ConfirmUpdateDirective` を画面コンポーネントの `imports` に追加します。

```typescript
import { ConfirmUpdateDirective } from "../component/dialog/confirm-update.directive";

@Component({
    imports: [ConfirmUpdateDirective],
    // ...
})
```

対象フォームに確認文言を指定します。通常のフォーム送信、Angular の `(submit)` による API 呼び出しのどちらにも使用できます。

```html
<form confirmUpdate="パスワードを変更しますか？" (submit)="changePassword($event, form)" #form>
    <!-- 入力欄 -->
    <button type="submit">パスワードを変更</button>
</form>
```

文言を空にすると、押された送信ボタンの表示から「『一括承認』を実行しますか？」などの確認文言を生成します。

```html
<form confirmUpdate="" method="post" [action]="action">
    <button type="submit" name="decision" value="APPROVED">一括承認</button>
    <button type="submit" name="decision" value="REJECTED">一括差戻し</button>
</form>
```

必須項目などの HTML 入力チェックを通過すると確認を表示し、「実行する」を押した場合だけ送信処理を再開します。
押されたボタンの `name` / `value`、CSRF トークン、ファイル入力などは元のフォームのまま送信します。
キャンセル・Esc では入力内容を保持し、サーバーへの送信もローディング表示も行いません。

`form.submit()` は送信イベントを発火しないため、この確認を通りません。画面の通常の送信ボタンまたは `form.requestSubmit()` を使用してください。
既存の `window.confirm()` を同じフォームに残すと確認が重複するため、置き換える際は削除します。
