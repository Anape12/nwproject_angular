import { Component } from "@angular/core";
import { projectRequest } from "../infr/project-request";

@Component({
    selector: "nw-qiita-unavailable",
    templateUrl: "./qiita-unavailable.html",
    host: { style: "display: contents" },
})
export class QiitaUnavailableComponent {
    readonly project = projectRequest;
}
