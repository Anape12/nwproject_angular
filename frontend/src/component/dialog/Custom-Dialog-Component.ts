import { Component, ElementRef, Input, output, viewChild } from "@angular/core";

@Component({
    selector: "app-custom-dialog",
    standalone: true,
    templateUrl: "./CustomDialogComponent.html",
    styleUrl: "./CustomDialogComponent.css",
})
export class CustomDialogComponent {
    @Input() title = "実行の確認";
    @Input() message = "この操作を実行しますか？";
    readonly confirmed = output<void>();
    private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>("dialog");

    showDialog(): void {
        this.dialog().nativeElement.showModal();
    }

    confirm(): void {
        const dialog = this.dialog().nativeElement;
        if (!dialog.open) return;
        dialog.close();
        this.confirmed.emit();
    }

    cancel(event?: Event): void {
        event?.preventDefault();
        this.dialog().nativeElement.close();
    }
}
