import { Component, ElementRef, inject } from "@angular/core";

@Component({
    selector: "nw-business-menu",
    templateUrl: "./business-menu.html",
    host: { style: "display: contents" },
})
export class BusinessMenuComponent {
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}
