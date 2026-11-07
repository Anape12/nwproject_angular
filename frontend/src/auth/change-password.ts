import { Component, ElementRef, inject, signal } from "@angular/core";
import { requestWithLoginContext } from "../shared/login-context";

@Component({
    selector: "nw-change-password",
    templateUrl: "./change-password.html",
    host: { style: "display: contents" },
})
export class ChangePasswordComponent {
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
    readonly errorMessage = signal("");
    readonly submitting = signal(false);

    async changePassword(event: Event, form: HTMLFormElement): Promise<void> {
        event.preventDefault();
        if (this.submitting()) return;
        this.submitting.set(true);
        this.errorMessage.set("");
        try {
            const body = new URLSearchParams();
            for (const [key, value] of new FormData(form)) {
                if (typeof value === "string") body.append(key, value);
            }
            const response = await requestWithLoginContext(this.contextPath + "/ChangePassword?format=json", {
                method: "POST", mode: "same-origin", body,
            });
            if (response.redirected) {
                location.assign(response.url);
                return;
            }
            if (response.status === 400) {
                const { message } = await response.json();
                this.errorMessage.set(message);
                return;
            }
            if (!response.ok) throw new Error("Password change failed");
            const { destination } = await response.json();
            location.assign(destination);
        } catch {
            this.errorMessage.set("パスワードを変更できませんでした。再度お試しください。");
        } finally {
            form.reset();
            this.submitting.set(false);
        }
    }
}
