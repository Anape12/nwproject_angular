import { Component, DestroyRef, ElementRef, inject } from "@angular/core";

interface ThreadView {
    id: number;
    title: string;
    authorId: string;
    content: string;
    status: string | null;
    closed: boolean;
    closedById: string | null;
    closedAt: string | null;
    canManage: boolean;
    comments: { id: number; authorId: string; text: string; createdAt: string | null; mine: boolean; ai: boolean }[];
}

@Component({
    selector: "nw-thread-detail",
    templateUrl: "./thread-detail.html",
    host: { style: "display: contents" },
})
export class ThreadDetailComponent {
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
    readonly csrfToken = this.data["csrfToken"] ?? "";
    readonly flash = this.data["flash"] ?? "";
    readonly flashType = this.data["flashType"] ?? "";
    readonly pending = this.data["pending"] === "true";
    readonly thread: ThreadView = JSON.parse(this.data["thread"] ?? "{}");

    constructor() {
        const destroyRef = inject(DestroyRef);
        if (this.pending) {
            const reload = window.setTimeout(() => location.reload(), 3000);
            destroyRef.onDestroy(() => window.clearTimeout(reload));
        }
    }

    confirmStatusChange(event: Event): void {
        const message = this.thread.closed ? "このスレッドを再開しますか？" : "このスレッドを完了にしますか？";
        if (!window.confirm(message)) event.preventDefault();
    }
}
