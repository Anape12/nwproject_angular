import { bootstrapApplication } from "@angular/platform-browser";
import { AiCharacterAdminComponent } from "./admin/ai-character-admin";
import { AttendanceComponent } from "./attendance/attendance";
import { AuditLogComponent } from "./admin/audit-log";
import { ChatRoomComponent } from "./communication/chat-room";
import { ChatCreateComponent, ChatListComponent, ChatMembersComponent, ThreadListComponent } from "./communication/communication-pages";
import { ExternalAppComponent } from "./integrations/external-app";
import { CommunicationMenuComponent, HeaderComponent, MenuSelectComponent, prepareStaticPage } from "./navigation/navigation";
import { PortalComponent } from "./navigation/portal";
import { ReportApprovalComponent } from "./reports/report-approval";
import { ReportWriteComponent } from "./reports/report-write";
import { ReverseEngineeringComponent } from "./admin/reverse-engineering";
import { ScheduleCalendarComponent } from "./attendance/schedule-calendar";
import { ThreadDetailComponent } from "./communication/thread-detail";
import { UserSearchComponent } from "./users/user-search";
import { UserSecurityComponent } from "./users/user-security";
import { UserRegistrationComponent } from "./users/user-registration";
import { BusinessMenuComponent } from "./navigation/business-menu";
import { ChangePasswordComponent } from "./auth/change-password";
import { LoginComponent } from "./auth/login";
import { NewThreadComponent } from "./communication/new-thread";
import { QiitaUnavailableComponent } from "./integrations/qiita-unavailable";
import { UserBirthdayComponent } from "./users/user-birthday";

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

async function startPage(): Promise<void> {
    const page = document.querySelector<HTMLElement>("[data-static-page]");
    if (page) {
        try {
            if (!await prepareStaticPage(page)) return;
        } catch {
            page.setAttribute("role", "alert");
            page.textContent = "画面を読み込めませんでした。ページを再読み込みしてください。";
            return;
        }
    }
    for (const [selector, component] of entryPoints) {
        if (document.querySelector(selector)) {
            bootstrapApplication(component).catch((error: unknown) => console.error(error));
        }
    }
}

void startPage();
