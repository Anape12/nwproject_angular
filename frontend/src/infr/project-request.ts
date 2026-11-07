import { OnInit, signal } from "@angular/core";

export class ProjectRequest implements OnInit {
    readonly requestUrl = location.pathname;
    readonly errorMessage = signal("")

    async ngOnInit(): Promise<void> {
    }
}