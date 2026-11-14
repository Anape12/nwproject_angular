import { Component, Input } from "@angular/core";

@Component({
    selector: "app-custom-button",
    standalone: true,
    templateUrl: "./CustomButtonComponent.html",
})
export class CustomButtonComponent {
    @Input({ required: true }) name!: string;
    @Input() btnValue = "ボタン";
    @Input() btnId = "";
    @Input() content = "";
    @Input() trgClass = "";
    @Input() buttonType: "button" | "submit" | "reset" = "button";
    @Input() required = false;
    @Input() disabled = false;

    public onFocus(event: FocusEvent): void {
        // フォーカスされたとき
        console.log("onFocus", event);
    }

    public onBlur(event: FocusEvent, btnObject: Object): void {
        // フォーカスが外れた時、入力チェック
        console.log("onBlur", event, btnObject);
    }
}