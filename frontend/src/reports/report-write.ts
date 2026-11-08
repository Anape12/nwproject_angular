import { Component, ElementRef, inject } from "@angular/core";
import { projectRequest } from "../infr/project-request";

interface ReportRow {
    id: string;
    title: string;
    date: string;
    updatedAtLabel: string;
    status: string;
    statusLabel: string;
}

interface ReportDetail {
    report: ReportRow;
    editable: boolean;
    body: string | null;
    reviewComment: string | null;
    reviewerName: string | null;
    reviewedAtLabel: string;
}

@Component({
    selector: "nw-report-write",
    templateUrl: "./report-write.html",
    host: { style: "display: contents" },
})
export class ReportWriteComponent {
    readonly project = projectRequest;
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly action = projectRequest.url("reportWrite");
    readonly csrfToken = this.data["csrfToken"] ?? "";
    readonly attachmentCsrf = this.data["attachmentCsrf"] ?? "";
    readonly today = this.data["today"] ?? "";
    readonly flash = this.data["flash"] ?? "";
    readonly flashType = this.data["flashType"] ?? "";
    readonly reports: ReportRow[] = JSON.parse(this.data["reports"] || "[]");
    readonly selected: ReportDetail | null = JSON.parse(this.data["selected"] || "null");
    readonly attachments: { id: string; name: string }[] = JSON.parse(this.data["attachments"] || "[]");

    confirmDelete(event: Event): void {
        if (!window.confirm("この下書きを削除しますか？")) event.preventDefault();
    }
}
