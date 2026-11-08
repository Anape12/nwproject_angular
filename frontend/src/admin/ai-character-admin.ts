import { Component, ElementRef, inject } from "@angular/core";
import { projectRequest } from "../infr/project-request";

interface CharacterRow {
    id: string;
    userId: string;
    name: string;
    promptKey: string;
    modelName: string | null;
    replyMode: string;
    active: boolean;
}

interface JobRow {
    id: string;
    characterName: string;
    sourceType: string;
    status: string;
    retryCount: number;
    errorMessage: string | null;
    createdAt: string;
}

@Component({
    selector: "nw-ai-character-admin",
    templateUrl: "./ai-character-admin.html",
    host: { style: "display: contents" },
})
export class AiCharacterAdminComponent {
    private readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly action = projectRequest.url("aiCharacterAdmin");
    readonly csrfToken = this.data["csrfToken"] ?? "";
    readonly flash = this.data["flash"] ?? "";
    readonly flashType = this.data["flashType"] ?? "";
    readonly characters: CharacterRow[] = JSON.parse(this.data["characters"] || "[]");
    readonly selected: CharacterRow | null = JSON.parse(this.data["selected"] || "null");
    readonly jobs: JobRow[] = JSON.parse(this.data["jobs"] || "[]");
}
