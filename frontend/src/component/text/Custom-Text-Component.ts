import { Component, Input, signal } from "@angular/core";
import { CustomPopupComponent } from "../popup/Custom-Popup-Component";

import { CustomTextType } from "./Custom-Text-Type";

@Component({
    selector: "app-custom-text",
    standalone: true,
    imports: [CustomPopupComponent],
    templateUrl: "./CustomTextComponent.html",
    styleUrl: "./CustomTextComponent.css",
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

    readonly errorMessage = signal<string | null>(null);

    public onFocus(): void {
        this.errorMessage.set(null);
    }

    public onBlur(inputValue: string): void {
        // フォーカスが外れた時、入力チェック
        this.outValueCheck(inputValue);
    }

    /**
     * テキストのタイプによって、フォーカスアウト時のチェックを実施する
     * @param inputValue
     */
    public outValueCheck(inputValue: string): void {

        // エラーメッセージを初期化
        this.errorMessage.set(null);

        switch (this.customType) {
            case CustomTextType.NORMAL:
                // 通常のテキスト入力の場合のチェック処理を実装する
                break;
            case CustomTextType.HALF_WIDTH_ALPHANUMERIC:
                this.halfWidthAlphanumericCheck(inputValue);
                break;
        }
    }

    /**
     * 半角英数字のパターンチェック
     * @param inputValue
     * @returns
     */
    private halfWidthAlphanumericCheck(inputValue: string): boolean {
        // 半角英数字のチェック処理を実装する
        if (!inputValue.match(/^[A-Za-z0-9]*$/)) {
            this.errorMessage.set("半角英数字のみで入力してください。");
            return true;
        }
        return false;
    }


    // バリデーションチェックの参考：https://qiita.com/teshimaaaaa1101/items/1494906d04ab7c5f667d
    // テキストのタイプにより、フォーカスアウト時のチェックを実施する（構想：テキストのタイプ（半角英数字・全角文字・数字の場合カンマ区切り・全角数値を半角数値に））
    // 入力中の文字から、残り5文字とかになったら後何文字入力できるかを表示する（構想：残り文字数の表示）
    // 他の値との整合性チェック（時刻の場合、前後の逆転チェック・新規登録ではユーザー入力されているがパスワードが無い場合は強調表示とか）

}
