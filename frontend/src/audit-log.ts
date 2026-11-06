import { Component, ElementRef, inject, OnInit, signal } from "@angular/core";
import { loadPageData } from "./page-data";

interface AuditLogRow {
    createdAt: string | null;
    eventCategory: string;
    eventAction: string;
    actorUserId: string | null;
    targetType: string | null;
    targetId: string | null;
    success: boolean;
    ipAddress: string | null;
    displayDetail: string | null;
}

@Component({
    selector: "nw-audit-log",
    templateUrl: "./audit-log.html",
    host: { style: "display: contents" },
})
export class AuditLogComponent implements OnInit {
    readonly contextPath = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset["contextPath"] ?? "";
    readonly query = new URLSearchParams(location.search);
    readonly categories = ["AUTH", "USER", "SECURITY", "ATTENDANCE", "APPROVAL"];
    readonly logs = signal<AuditLogRow[] | null>(null);
    readonly loadFailed = signal(false);

    async ngOnInit(): Promise<void> {
        try {
            this.logs.set(await loadPageData<AuditLogRow[]>(this.contextPath, "/AuditLog", this.query));
        } catch {
            this.loadFailed.set(true);
        }
    }
}
