import { Component } from "@angular/core";
import { projectRequest } from "../infr/project-request";

@Component({
    selector: "nw-new-thread",
    templateUrl: "./new-thread.html",
    host: { style: "display: contents" },
})
export class NewThreadComponent {
    readonly project = projectRequest;
}
