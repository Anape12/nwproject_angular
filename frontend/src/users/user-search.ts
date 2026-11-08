import { Component, signal } from "@angular/core";
import { projectRequest } from "../infr/project-request";

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
    readonly searchUrl = projectRequest.url("userSearch");
    readonly searchFailed = signal(false);
    readonly searching = signal(false);
    readonly results = signal<SearchRow[] | null>(null);

    async search(event: Event, userId: string): Promise<void> {
        event.preventDefault();
        if (this.searching()) return;
        this.searching.set(true);
        this.searchFailed.set(false);
        this.results.set(null);
        try {
            const response = await projectRequest.request("userSearch", {
                method: "POST", body: new URLSearchParams({ userId }),
            });
            if (response.redirected) {
                location.assign(response.url);
                return;
            }
            if (!response.ok) throw new Error("User search failed");
            this.results.set(await response.json());
        } catch {
            this.searchFailed.set(true);
        } finally {
            this.searching.set(false);
        }
    }

    closeWindow(): void {
        window.close();
    }
}
