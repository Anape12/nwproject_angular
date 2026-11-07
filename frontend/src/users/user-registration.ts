import { Component, ElementRef, inject, signal } from "@angular/core";
import { requestWithLoginContext } from "../shared/login-context";

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
    readonly error = signal("");
    readonly submitting = signal(false);

    async register(event: Event, form: HTMLFormElement): Promise<void> {
        event.preventDefault();
        if (this.submitting()) return;
        this.submitting.set(true);
        this.error.set("");
        try {
            const body = new URLSearchParams();
            for (const [key, value] of new FormData(form)) {
                if (typeof value === "string") body.append(key, value);
            }
            const response = await requestWithLoginContext(this.action + "?format=json", { method: "POST", mode: "same-origin", body });
            if (response.redirected) {
                location.assign(response.url);
                return;
            }
            if (response.status === 400) {
                const { message } = await response.json();
                this.error.set(message);
                return;
            }
            if (!response.ok) throw new Error("Registration failed");
            const { destination } = await response.json();
            location.assign(destination);
        } catch {
            this.error.set("ユーザーを登録できませんでした。再度お試しください。");
        } finally {
            for (const name of ["password", "passwordConfirmation"]) {
                const field = form.elements.namedItem(name);
                if (field instanceof HTMLInputElement) field.value = "";
            }
            this.showPassword.set(false);
            this.showConfirmation.set(false);
            this.submitting.set(false);
        }
    }

    closeWindow(): void {
        window.close();
    }
}
