import { Component, ElementRef, inject } from "@angular/core";

interface SearchRow {
    id: number;
    userId: string;
    permission: string | null;
}

@Component({
    selector: "nw-user-search",
    templateUrl: "./user-search.html",
    host: { style: "display: contents" },
})
export class UserSearchComponent {
    private readonly attributes = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly searchUrl = (this.attributes["contextPath"] ?? "") + "/UserSearch";
    readonly userId = this.attributes["userId"] ?? "";
    readonly searchFailed = this.attributes["searchFailed"] === "true";
    readonly results: SearchRow[] | null = JSON.parse(this.attributes["results"] || "null");

    editUrl(userId: string): string {
        return this.searchUrl + "?action=edit&" + new URLSearchParams({ userId }).toString();
    }

    closeWindow(): void {
        window.close();
    }
}
