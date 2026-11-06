import { Component, ElementRef, inject, signal } from "@angular/core";

@Component({
    selector: "nw-user-registration",
    templateUrl: "./user-registration.html",
    host: { style: "display: contents" },
})
export class UserRegistrationComponent {
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly action = (this.data["contextPath"] ?? "") + "/UserInsert";
    readonly permissions: { id: string; label: string }[] = JSON.parse(this.data["permissions"] || "[]");
    readonly showPassword = signal(false);
    readonly showConfirmation = signal(false);

    closeWindow(): void {
        window.close();
    }
}
