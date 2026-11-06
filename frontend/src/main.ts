import { Component, OnInit, signal } from "@angular/core";
import { bootstrapApplication } from "@angular/platform-browser";
import { AiCharacterAdminComponent } from "./ai-character-admin";
import { AttendanceComponent } from "./attendance";
import { AuditLogComponent } from "./audit-log";
import { ChatRoomComponent } from "./chat-room";
import { ChatCreateComponent, ChatListComponent, ChatMembersComponent, ThreadListComponent } from "./communication-pages";
import { ExternalAppComponent } from "./external-app";
import { CommunicationMenuComponent, HeaderComponent, MenuSelectComponent, prepareStaticMenu } from "./navigation";
import { PortalComponent } from "./portal";
import { ReportApprovalComponent } from "./report-approval";
import { ReportWriteComponent } from "./report-write";
import { ReverseEngineeringComponent } from "./reverse-engineering";
import { ScheduleCalendarComponent } from "./schedule-calendar";
import { ThreadDetailComponent } from "./thread-detail";
import { UserSearchComponent } from "./user-search";
import { UserSecurityComponent } from "./user-security";
import { UserRegistrationComponent } from "./user-registration";
import {
    BusinessMenuComponent,
    ChangePasswordComponent,
    NewThreadComponent,
    QiitaUnavailableComponent,
    ResultComponent,
    UserBirthdayComponent,
} from "./migrated-pages";

@Component({
    selector: "nw-login",
    templateUrl: "./login.html",
    host: { style: "display: contents" },
})
class LoginComponent implements OnInit {
    readonly loginUrl = location.pathname;
    readonly errorMessage = signal("");
    readonly failed = signal(false);
    readonly submitting = signal(false);

    async ngOnInit(): Promise<void> {
        const query = new URLSearchParams(location.search);
        if (!query.has("sessionInvalid") && !query.has("windowInvalid")) return;
        query.set("format", "json");
        try {
            const response = await fetch(this.loginUrl + "?" + query, { mode: "same-origin" });
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
            const response = await fetch(this.loginUrl + "?format=json", {
                method: "POST",
                mode: "same-origin",
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

const entryPoints = [
    ["nw-login", LoginComponent],
    ["nw-menu-select", MenuSelectComponent],
    ["nw-communication-menu", CommunicationMenuComponent],
    ["nw-header", HeaderComponent],
    ["nw-audit-log", AuditLogComponent],
    ["nw-portal", PortalComponent],
    ["nw-user-search", UserSearchComponent],
    ["nw-change-password", ChangePasswordComponent],
    ["nw-user-birthday", UserBirthdayComponent],
    ["nw-new-thread", NewThreadComponent],
    ["nw-qiita-unavailable", QiitaUnavailableComponent],
    ["nw-business-menu", BusinessMenuComponent],
    ["nw-result", ResultComponent],
    ["nw-thread-list", ThreadListComponent],
    ["nw-chat-list", ChatListComponent],
    ["nw-chat-create", ChatCreateComponent],
    ["nw-reverse-engineering", ReverseEngineeringComponent],
    ["nw-thread-detail", ThreadDetailComponent],
    ["nw-chat-room", ChatRoomComponent],
    ["nw-chat-members", ChatMembersComponent],
    ["nw-user-security", UserSecurityComponent],
    ["nw-user-registration", UserRegistrationComponent],
    ["nw-ai-character-admin", AiCharacterAdminComponent],
    ["nw-report-approval", ReportApprovalComponent],
    ["nw-report-write", ReportWriteComponent],
    ["nw-schedule-calendar", ScheduleCalendarComponent],
    ["nw-attendance", AttendanceComponent],
    ["nw-external-app", ExternalAppComponent],
] as const;

for (const [selector, component] of entryPoints) {
    const host = document.querySelector<HTMLElement>(selector);
    if (!host) continue;
    if (host.hasAttribute("data-static-menu")) {
        prepareStaticMenu(host).then(ready => {
            if (ready) return bootstrapApplication(component);
        }).catch(() => {
            host.setAttribute("role", "alert");
            host.textContent = "メニューを読み込めませんでした。ページを再読み込みしてください。";
        });
    } else {
        bootstrapApplication(component).catch((error: unknown) => console.error(error));
    }
}
