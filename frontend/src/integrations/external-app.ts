import { Component, ElementRef, inject } from "@angular/core";

@Component({
    selector: "nw-external-app",
    templateUrl: "./external-app.html",
    host: { style: "display: contents" },
})
export class ExternalAppComponent {
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly appName = this.data["appName"] ?? "";
    readonly readMode = this.data["readMode"] === "true";
    readonly uploadUrl = (this.data["contextPath"] ?? "") + (this.readMode ? "/UploadReadServlet" : "/UploadServlet");
    readonly sampleRows = ["a0001", "a0002", "a0003", "a0004", "a0005"];

    constructor() {
        document.title = `メインメニュー（${this.appName}）`;
    }

    goBack(): void {
        history.back();
    }
}
