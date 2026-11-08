import { Component, ElementRef, inject } from "@angular/core";
import { projectRequest } from "../infr/project-request";

@Component({
    selector: "nw-business-menu",
    templateUrl: "./business-menu.html",
    host: { style: "display: contents" },
})
export class BusinessMenuComponent {
    readonly project = projectRequest;
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
}
