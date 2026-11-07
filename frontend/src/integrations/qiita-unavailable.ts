import { Component, ElementRef, inject } from "@angular/core";

@Component({
    selector: "nw-qiita-unavailable",
    templateUrl: "./qiita-unavailable.html",
    host: { style: "display: contents" },
})
export class QiitaUnavailableComponent {
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
}
