import { Component, ElementRef, inject } from "@angular/core";

interface ApplicationRow {
    id: string;
    title: string;
    typeLabel: string;
    applicantName: string;
    targetDate: string;
    status: string;
    statusLabel: string;
}

interface ApplicationDetail {
    application: ApplicationRow;
    detail: string;
    reviewComment: string | null;
    reviewerName: string | null;
    reviewedAtLabel: string;
    history: { action: string; actor: string; actedAt: string; comment: string | null }[];
}

@Component({
    selector: "nw-report-approval",
    templateUrl: "./report-approval.html",
    host: { style: "display: contents" },
})
export class ReportApprovalComponent {
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly action = (this.data["contextPath"] ?? "") + "/ReportApproval";
    readonly reviewAction = location.pathname + location.search;
    readonly csrfToken = this.data["csrfToken"] ?? "";
    readonly flash = this.data["flash"] ?? "";
    readonly flashType = this.data["flashType"] ?? "";
    readonly canConfigure = this.data["canConfigure"] === "true";
    readonly applications: ApplicationRow[] = JSON.parse(this.data["applications"] || "[]");
    readonly selected: ApplicationDetail | null = JSON.parse(this.data["selected"] || "null");
}
