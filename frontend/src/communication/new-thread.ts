import { Component, ElementRef, inject } from "@angular/core";

@Component({
    selector: "nw-new-thread",
    templateUrl: "./new-thread.html",
    host: { style: "display: contents" },
})
export class NewThreadComponent {
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}
