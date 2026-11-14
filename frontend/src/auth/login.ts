import { Component, OnInit, signal } from "@angular/core";
import { projectRequest } from "../infr/project-request";

import { CustomTextComponent } from "../component/text/Custom-Text-Component";

@Component({
    imports: [CustomTextComponent],
    selector: "nw-login",
    templateUrl: "./login.html",
    host: { style: "display: contents" },
})
export class LoginComponent implements OnInit {
    readonly loginUrl = projectRequest.url("current");
    readonly errorMessage = signal("");
    readonly failed = signal(false);
    readonly submitting = signal(false);

    async ngOnInit(): Promise<void> {
        const query = new URLSearchParams(location.search);
        if (!query.has("sessionInvalid") && !query.has("windowInvalid")) return;
        try {
            const response = await projectRequest.request("current", { query, json: true });
            if (response.ok && !this.submitting() && !this.failed()) {
                const { message } = await response.json();
                if (!this.submitting() && !this.failed()) this.errorMessage.set(message);
            }
        } catch {
            // The form remains usable if the informational message cannot load.
        }
    }

    async submit(event: Event, form: HTMLFormElement): Promise<void> {
        event.preventDefault();
        if (this.submitting()) return;
        this.submitting.set(true);
        try {
            const values = new FormData(form);
            const response = await projectRequest.request("current", {
                json: true,
                method: "POST",
                body: new URLSearchParams({
                    userId: String(values.get("userId") ?? ""),
                    password: String(values.get("password") ?? ""),
                }),
            });
            if (response.status === 401) {
                const { message } = await response.json();
                this.errorMessage.set(message);
                this.failed.set(true);
                document.body.classList.add("auth-result-page");
                document.title = "ログインできませんでした | NW Project";
            } else if (response.ok) {
                const { destination } = await response.json();
                location.assign(destination);
            } else {
                this.errorMessage.set("ログイン処理に失敗しました。再度お試しください。");
            }
        } catch {
            this.errorMessage.set("ログイン処理に失敗しました。再度お試しください。");
        } finally {
            this.submitting.set(false);
        }
    }
}
