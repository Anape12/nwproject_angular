import { ComponentRef, DestroyRef, Directive, ElementRef, Input, ViewContainerRef, inject } from "@angular/core";
import { CustomDialogComponent } from "./Custom-Dialog-Component";

/** Opt in only on forms that commit a change, including forms with Angular submit handlers. */
@Directive({
    selector: "form[confirmUpdate]",
    standalone: true,
})
export class ConfirmUpdateDirective {
    @Input() confirmUpdate = "入力内容を登録・更新しますか？";
    private readonly form = inject<ElementRef<HTMLFormElement>>(ElementRef).nativeElement;
    private readonly container = inject(ViewContainerRef);
    private dialog?: ComponentRef<CustomDialogComponent>;
    private approved = false;

    constructor() {
        this.form.dataset["nwConfirmationRequired"] = "true";
        // Capture before Angular submit handlers so a cancelled action cannot call an API.
        this.form.addEventListener("submit", this.onSubmit, true);
        inject(DestroyRef).onDestroy(() => {
            this.form.removeEventListener("submit", this.onSubmit, true);
            delete this.form.dataset["nwConfirmationRequired"];
        });
    }

    private readonly onSubmit = (event: SubmitEvent): void => {
        if (this.approved || event.defaultPrevented) return;
        event.preventDefault();
        event.stopImmediatePropagation();

        this.dialog?.destroy();
        const dialog = this.container.createComponent(CustomDialogComponent);
        this.dialog = dialog;
        const submitter = event.submitter instanceof HTMLButtonElement || event.submitter instanceof HTMLInputElement
            ? event.submitter : null;
        const label = (submitter instanceof HTMLInputElement ? submitter.value : submitter?.textContent)?.trim();
        dialog.setInput("title", "更新の確認");
        dialog.setInput("message", this.confirmUpdate || `「${label || "この操作"}」を実行しますか？`);
        dialog.instance.confirmed.subscribe(() => {
            if (!this.form.isConnected || (submitter && submitter.form !== this.form)) return;
            this.approved = true;
            this.form.dataset["nwConfirmationRequired"] = "false";
            try {
                // Preserve validation and the clicked button's name/value (approve/reject etc.).
                this.form.requestSubmit(submitter);
            } finally {
                this.approved = false;
                this.form.dataset["nwConfirmationRequired"] = "true";
            }
        });
        dialog.changeDetectorRef.detectChanges();
        dialog.instance.showDialog();
    };
}
