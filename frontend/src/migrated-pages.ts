import { Component, ElementRef, inject } from "@angular/core";

@Component({
    selector: "nw-change-password",
    templateUrl: "./change-password.html",
    host: { style: "display: contents" },
})
export class ChangePasswordComponent {
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}

@Component({
    selector: "nw-user-birthday",
    templateUrl: "./user-birthday.html",
    host: { style: "display: contents" },
})
export class UserBirthdayComponent {
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}

@Component({
    selector: "nw-new-thread",
    templateUrl: "./new-thread.html",
    host: { style: "display: contents" },
})
export class NewThreadComponent {
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}

@Component({
    selector: "nw-qiita-unavailable",
    templateUrl: "./qiita-unavailable.html",
    host: { style: "display: contents" },
})
export class QiitaUnavailableComponent {
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}

@Component({
    selector: "nw-business-menu",
    templateUrl: "./business-menu.html",
    host: { style: "display: contents" },
})
export class BusinessMenuComponent {
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}

@Component({
    selector: "nw-result",
    templateUrl: "./result.html",
    host: { style: "display: contents" },
})
export class ResultComponent {
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}
