import { Component, ElementRef, inject } from "@angular/core";
import { loadPageData } from "./page-data";

interface MenuData {
    contextPath: string;
    firstName: string | null;
    lastName: string | null;
    loginContext: string;
}

export async function prepareStaticMenu(host: HTMLElement): Promise<boolean> {
    const data = await loadPageData<MenuData>("", location.pathname, new URLSearchParams(location.search));
    if (!data) return false;
    host.dataset["contextPath"] = data.contextPath;
    host.dataset["firstName"] = data.firstName ?? "";
    host.dataset["lastName"] = data.lastName ?? "";

    Object.assign(window, { NW_SERVER_LOGIN_CONTEXT: data.loginContext, NW_CONTEXT_PATH: data.contextPath });
    await new Promise<void>((resolve, reject) => {
        const script = document.createElement("script");
        script.src = data.contextPath + "/js/login-context.js?v=20261107-1";
        script.onload = () => resolve();
        script.onerror = () => reject(new Error("Login context script failed to load"));
        document.head.appendChild(script);
    });
    return true;
}

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
