import { Component, ElementRef, inject, signal } from "@angular/core";
import { loadPageData } from "../shared/page-data";
import { requestWithLoginContext } from "../shared/login-context";

function pageAttributes(): DOMStringMap {
    return inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
}

function loadRows<T>(contextPath: string, path: string) {
    const rows = signal<T[] | null>(null);
    const failed = signal(false);
    void loadPageData<T[]>(contextPath, path, new URLSearchParams(location.search))
        .then(value => rows.set(value))
        .catch(() => failed.set(true));
    return { rows, failed };
}

interface ThreadRow {
    threadId: number;
    title: string;
    content: string;
    authorId: string;
    status: string | null;
    closed: boolean;
}

interface RoomRow {
    roomId: string;
    displayName: string | null;
    direct: boolean;
    hasAi: boolean;
}

interface ChatUserRow {
    userId: string;
    firstName: string;
    lastName: string;
    ai: boolean;
}

@Component({
    selector: "nw-thread-list",
    templateUrl: "./thread-list.html",
    host: { style: "display: contents" },
})
export class ThreadListComponent {
    private readonly data = pageAttributes();
    readonly contextPath = this.data["contextPath"] ?? "";
    readonly list = loadRows<ThreadRow>(this.contextPath, "/ThreadController");
}

@Component({
    selector: "nw-chat-list",
    templateUrl: "./chat-list.html",
    host: { style: "display: contents" },
})
export class ChatListComponent {
    private readonly data = pageAttributes();
    readonly contextPath = this.data["contextPath"] ?? "";
    readonly list = loadRows<RoomRow>(this.contextPath, "/ChatChanelList");

    roomUrl(room: RoomRow): string {
        const query = new URLSearchParams({ roomId: room.roomId, displayName: room.displayName ?? "" });
        return this.contextPath + "/ChatChanelRoom?" + query;
    }
}

@Component({
    selector: "nw-chat-create",
    templateUrl: "./chat-create.html",
    host: { style: "display: contents" },
})
export class ChatCreateComponent {
    private readonly data = pageAttributes();
    readonly contextPath = this.data["contextPath"] ?? "";
    readonly errorMessage = signal("");
    readonly submitting = signal(false);
    readonly users: ChatUserRow[] = JSON.parse(this.data["rows"] || "[]");

    async create(event: Event, targetUserId: string): Promise<void> {
        event.preventDefault();
        if (this.submitting()) return;
        this.submitting.set(true);
        this.errorMessage.set("");
        try {
            const response = await requestWithLoginContext(this.contextPath + "/ChatRoomCreate?format=json", {
                method: "POST",
                mode: "same-origin",
                body: new URLSearchParams({ targetUserId }),
            });
            if (response.redirected) {
                location.assign(response.url);
                return;
            }
            if (response.status === 400) {
                const { message } = await response.json();
                this.errorMessage.set(message);
                return;
            }
            if (!response.ok) throw new Error("Chat creation failed");
            const { destination } = await response.json();
            location.assign(destination);
        } catch {
            this.errorMessage.set("チャットを開始できませんでした。再度お試しください。");
        } finally {
            this.submitting.set(false);
        }
    }
}

@Component({
    selector: "nw-chat-members",
    templateUrl: "./chat-members.html",
    host: { style: "display: contents" },
})
export class ChatMembersComponent {
    private readonly data = pageAttributes();
    readonly contextPath = this.data["contextPath"] ?? "";
    readonly roomId = this.data["roomId"] ?? "";
    readonly roomUrl = this.contextPath + "/ChatChanelRoom?" + new URLSearchParams({ roomId: this.roomId });
    readonly csrfToken = this.data["csrfToken"] ?? "";
    readonly flash = this.data["flash"] ?? "";
    readonly members: ChatUserRow[] = JSON.parse(this.data["members"] || "[]");
    readonly available: ChatUserRow[] = JSON.parse(this.data["available"] || "[]");
}
