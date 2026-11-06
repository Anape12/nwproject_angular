import { Component, ElementRef, inject } from "@angular/core";

function readNavigationData() {
    const { contextPath, firstName, lastName, loggedIn } =
        inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;

    return {
        contextPath: contextPath ?? "",
        firstName: firstName ?? "",
        lastName: lastName ?? "",
        loggedIn: loggedIn === "true",
    };
}

@Component({
    selector: "nw-menu-select",
    templateUrl: "./menu-select.html",
    host: { style: "display: contents" },
})
export class MenuSelectComponent {
    readonly navigation = readNavigationData();
}

@Component({
    selector: "nw-communication-menu",
    templateUrl: "./communication-menu.html",
    host: { style: "display: contents" },
})
export class CommunicationMenuComponent {
    readonly navigation = readNavigationData();
}

@Component({
    selector: "nw-header",
    templateUrl: "./header.html",
    host: { style: "display: contents" },
})
export class HeaderComponent {
    readonly navigation = readNavigationData();
}
