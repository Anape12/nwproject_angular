import { Component, Input } from "@angular/core";

@Component({
    selector: "app-custom-popup",
    standalone: true,
    templateUrl: "./CustomPopupComponent.html",
    styleUrl: "./CustomPopupComponent.css",
})
export class CustomPopupComponent {
    @Input({ required: true }) popupId!: string;
    @Input({ required: true }) message!: string;
}
