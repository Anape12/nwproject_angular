import { Component, ElementRef, inject } from "@angular/core";
import { projectRequest } from "../infr/project-request";

@Component({
    selector: "nw-user-birthday",
    templateUrl: "./user-birthday.html",
    host: { style: "display: contents" },
})
export class UserBirthdayComponent {
    readonly project = projectRequest;
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
}
