import { Component, ElementRef, inject, OnInit, signal } from "@angular/core";
import { loadPageData } from "../shared/page-data";

interface PortalData {
    portalError?: string;
    dashboard: {
        attendance: { recorded: string; actionable: string | null; overtime: string };
        pendingApprovals: string;
        unreadCount: string;
        todayEvents: { event_id: string; title: string; start_at: string; color: string | null }[];
        notifications: { notification_id: string; title: string; message: string; link_url: string | null }[];
        recentReports: { report_id: string; title: string; report_date: string; status: string }[];
        myApprovals: { approval_id: string; application_type: string; status: string; submitted_at: string }[];
    };
    searchResults: { url: string; type: string; title: string; detail: string }[];
}

@Component({
    selector: "nw-portal",
    templateUrl: "./portal.html",
    host: { style: "display: contents" },
})
export class PortalComponent implements OnInit {
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
    readonly contextPath = this.host.dataset["contextPath"] ?? "";
    readonly portalError = signal("");
    readonly portalUrl = this.contextPath + "/Portal";
    private readonly query = new URLSearchParams(location.search);
    readonly searchTerm = this.query.get("q") ?? "";
    readonly data = signal<PortalData | null>(null);
    readonly loadFailed = signal(false);

    async ngOnInit(): Promise<void> {
        try {
            const data = await loadPageData<PortalData>(this.contextPath, "/Portal", this.query);
            this.portalError.set(data?.portalError ?? "");
            this.data.set(data);
        } catch {
            this.loadFailed.set(true);
        }
    }

    confirmWithdrawal(event: Event): void {
        if (!window.confirm("この申請を取り下げますか？")) {
            event.preventDefault();
        }
    }
}
