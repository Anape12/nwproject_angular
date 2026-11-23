import { Component } from "@angular/core";
import { ConfirmUpdateDirective } from "../component/dialog/confirm-update.directive";
import { projectRequest } from "../infr/project-request";

@Component({
    imports: [ConfirmUpdateDirective],
    selector: "nw-new-thread",
    templateUrl: "./new-thread.html",
    host: { style: "display: contents" },
})
export class NewThreadComponent {
    readonly project = projectRequest;
}
