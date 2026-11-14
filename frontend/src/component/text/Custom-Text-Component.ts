import { Component, Input } from "@angular/core";

@Component({
    selector: "app-custom-text",
    standalone: true,
    templateUrl: "./CustomTextComponent.html",
})
export class CustomTextComponent {
    @Input({ required: true }) name!: string;
    @Input() textType = "text";
    @Input() inputId = "";
    @Input() content = "";
    @Input() required = false;
    @Input() maxLength: number | null = null;
    @Input() autocomplete: string | null = null;
    @Input() variant: "primary" | "secondary" | "danger" = "primary";
    @Input() customType: string | null = "NORMAL";
 
    public onFocus(event: FocusEvent): void {
        // フォーカスされたとき
        console.log("onFocus", event);
    }

    public onBlur(event: FocusEvent, inputValue: string): void {
        // フォーカスが外れた時、入力チェック
        console.log("onBlur", event, inputValue);
        console.log("customType", this.customType);
    }

    // テキストのタイプにより、フォーカスアウト時のチェックを実施する（構想：テキストのタイプ（半角英数字・全角文字・数字の場合カンマ区切り・全角数値を半角数値に））
    // 入力中の文字から、残り5文字とかになったら後何文字入力できるかを表示する（構想：残り文字数の表示）
    // 他の値との整合性チェック（時刻の場合、前後の逆転チェック・新規登録ではユーザー入力されているがパスワードが無い場合は強調表示とか）

}