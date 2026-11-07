import { Component, ElementRef, inject } from "@angular/core";

@Component({
    selector: "nw-user-birthday",
    templateUrl: "./user-birthday.html",
    host: { style: "display: contents" },
})
export class UserBirthdayComponent {
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}
