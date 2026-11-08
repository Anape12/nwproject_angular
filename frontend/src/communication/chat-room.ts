import { afterNextRender, Component, DestroyRef, ElementRef, inject, Injector, signal, viewChild } from "@angular/core";
import { projectRequest } from "../infr/project-request";

interface ChatMessage {
    messageId: number;
    postedById: string;
    postedByName: string;
    postedByAccountType: string;
    message: string;
    createdAt: string;
}

interface Attachment {
    id: string;
    name: string;
}

@Component({
    selector: "nw-chat-room",
    templateUrl: "./chat-room.html",
    host: { style: "display: contents" },
})
export class ChatRoomComponent {
    readonly project = projectRequest;
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
    private readonly injector = inject(Injector);
    private readonly area = viewChild.required<ElementRef<HTMLElement>>("chatArea");
    readonly roomId = this.host.dataset["roomId"] ?? "";
    readonly roomName = this.host.dataset["roomName"] ?? "";
    readonly userId = this.host.dataset["userId"] ?? "";
    readonly csrfToken = this.host.dataset["csrfToken"] ?? "";
    readonly canManage = this.host.dataset["canManage"] === "true";
    readonly membersUrl = projectRequest.url("chatMembers", new URLSearchParams({ roomId: this.roomId }));
    readonly attachments: Attachment[] = JSON.parse(this.host.dataset["attachments"] || "[]");
    readonly messages = signal<ChatMessage[]>(JSON.parse(this.host.dataset["messages"] || "[]"));
    readonly sending = signal(false);
    readonly sendFailed = signal(false);
    private refreshing = false;
    private sendVersion = 0;

    constructor() {
        afterNextRender(() => this.scrollToBottom());
        const poll = window.setInterval(() => void this.refresh(), 3000);
        inject(DestroyRef).onDestroy(() => window.clearInterval(poll));
    }

    onKeydown(event: KeyboardEvent, form: HTMLFormElement): void {
        if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            form.requestSubmit();
        }
    }

    async send(event: Event, input: HTMLTextAreaElement): Promise<void> {
        event.preventDefault();
        if (this.sending()) return;
        this.sendVersion++;
        this.sending.set(true);
        this.sendFailed.set(false);
        const text = input.value;
        try {
            const response = await projectRequest.request("chatSend", {
                method: "POST",
                body: new URLSearchParams({ commentText: text, roomId: this.roomId }),
            });
            const messages = await this.readMessages(response);
            if (messages === null) return;
            this.messages.set(messages);
            afterNextRender(() => this.scrollToBottom(), { injector: this.injector });
            if (input.value === text) input.value = "";
        } catch {
            this.sendFailed.set(true);
        } finally {
            this.sending.set(false);
        }
    }

    private async refresh(): Promise<void> {
        if (document.hidden || this.sending() || this.refreshing) return;
        this.refreshing = true;
        const version = this.sendVersion;
        try {
            const query = new URLSearchParams({ roomId: this.roomId });
            const response = await projectRequest.request("chatMessages", { query });
            if (version !== this.sendVersion) return;
            const messages = await this.readMessages(response);
            if (messages === null || version !== this.sendVersion) return;
            const area = this.area().nativeElement;
            const scrollTop = area.scrollTop;
            const nearBottom = area.scrollHeight - area.clientHeight - scrollTop <= 80;
            this.messages.set(messages);
            afterNextRender(() => {
                if (nearBottom) this.scrollToBottom();
                else area.scrollTop = scrollTop;
            }, { injector: this.injector });
        } catch {
            // The next poll retries without replacing the visible messages.
        } finally {
            this.refreshing = false;
        }
    }

    private async readMessages(response: Response): Promise<ChatMessage[] | null> {
        if (response.redirected) {
            location.assign(response.url);
            return null;
        }
        if (!response.ok) throw new Error("Chat request failed");
        const messages = await response.json();
        if (!Array.isArray(messages)) throw new Error("Invalid chat response");
        return messages;
    }

    private scrollToBottom(): void {
        const area = this.area().nativeElement;
        area.scrollTop = area.scrollHeight;
    }
}
