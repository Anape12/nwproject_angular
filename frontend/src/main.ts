import { Component, ElementRef, inject } from "@angular/core";
import { bootstrapApplication } from "@angular/platform-browser";
import { AiCharacterAdminComponent } from "./ai-character-admin";
import { AttendanceComponent } from "./attendance";
import { AuditLogComponent } from "./audit-log";
import { ChatRoomComponent } from "./chat-room";
import { ChatCreateComponent, ChatListComponent, ChatMembersComponent, ThreadListComponent } from "./communication-pages";
import { ExternalAppComponent } from "./external-app";
import { CommunicationMenuComponent, HeaderComponent, MenuSelectComponent } from "./navigation";
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
class LoginComponent {
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
    readonly loginUrl = this.host.dataset["loginUrl"] ?? "Login";
    readonly errorMessage = this.host.dataset["errorMessage"] ?? "";
    readonly failed = this.host.dataset["failed"] === "true";
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
    if (document.querySelector(selector)) {
        bootstrapApplication(component).catch((error: unknown) => console.error(error));
    }
}
