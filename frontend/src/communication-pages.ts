import { Component, ElementRef, inject } from "@angular/core";

function pageAttributes(): DOMStringMap {
    return inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
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
    readonly threads: ThreadRow[] = JSON.parse(this.data["rows"] || "[]");
}

@Component({
    selector: "nw-chat-list",
    templateUrl: "./chat-list.html",
    host: { style: "display: contents" },
})
export class ChatListComponent {
    private readonly data = pageAttributes();
    readonly contextPath = this.data["contextPath"] ?? "";
    readonly rooms: RoomRow[] = JSON.parse(this.data["rows"] || "[]");

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
    readonly errorMessage = this.data["errorMessage"] ?? "";
    readonly users: ChatUserRow[] = JSON.parse(this.data["rows"] || "[]");
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
