import { Component, ElementRef, inject } from "@angular/core";

interface AccountRow {
    userId: string;
    firstName: string;
    lastName: string;
    accountType: string;
    permission: string;
    disabled: boolean;
    lastLoginAt: string | null;
    lockedUntil: string | null;
    failedLoginCount: number;
    forcePasswordChange: boolean;
    manageable: boolean;
}

@Component({
    selector: "nw-user-security",
    templateUrl: "./user-security.html",
    host: { style: "display: contents" },
})
export class UserSecurityComponent {
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
    readonly action = this.contextPath + "/UserSecurityAdmin";
    readonly csrf = this.data["csrfToken"] ?? "";
    readonly flash = this.data["flash"] ?? "";
    readonly flashType = this.data["flashType"] ?? "";
    readonly systemAdmin = this.data["systemAdmin"] === "true";
    readonly users: AccountRow[] = JSON.parse(this.data["users"] || "[]");

    birthdayUrl(userId: string): string {
        return this.contextPath + "/UserBirthday?" + new URLSearchParams({ userId });
    }
}
