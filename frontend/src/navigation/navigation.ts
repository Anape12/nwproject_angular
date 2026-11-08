import { Component, ElementRef, inject } from "@angular/core";
import { projectRequest } from "../infr/project-request";
import { initializeLoginContext } from "../shared/login-context";

interface MenuData {
    [key: string]: string | boolean | null;
    contextPath: string;
    firstName: string | null;
    lastName: string | null;
    loginContext: string;
}

export async function prepareStaticPage(host: HTMLElement): Promise<boolean> {
    const route = host.dataset["staticPage"] || "current";
    if (route !== "current" && route !== "menuSelect" && route !== "selectApp") {
        throw new Error("Unknown page bootstrap route");
    }
    const query = new URLSearchParams(location.search);
    if (route !== "current") {
        for (const key of [...query.keys()]) {
            if (key !== "_loginContext" && key !== "loginFresh") query.delete(key);
        }
    }
    const data = await projectRequest.loadPageData<MenuData>(route, query);
    if (!data) return false;
    const { loginContext, contextPath, firstName, lastName, ...pageData } = data;
    const navigation = { contextPath, firstName: firstName ?? "", lastName: lastName ?? "" };
    Object.assign(host.dataset, navigation);
    setPageData(host, pageData);
    const header = document.querySelector<HTMLElement>("nw-header");
    if (header) Object.assign(header.dataset, navigation, { loggedIn: "true" });

    projectRequest.setContextPath(contextPath);
    initializeLoginContext(loginContext);
    if (host.hasAttribute("data-page-json")) {
        const content = await projectRequest.loadPageData<Record<string, unknown>>(
            "current", new URLSearchParams(location.search),
        );
        if (!content) return false;
        setPageData(host, content);
    }
    return true;
}

function setPageData(host: HTMLElement, data: Record<string, unknown>): void {
    for (const [key, value] of Object.entries(data)) {
        host.dataset[key] = typeof value === "object" && value !== null ? JSON.stringify(value) : String(value ?? "");
    }
}

function readNavigationData() {
    const { firstName, lastName, loggedIn } =
        inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;

    return {
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
    readonly project = projectRequest;
    readonly navigation = readNavigationData();
}

@Component({
    selector: "nw-communication-menu",
    templateUrl: "./communication-menu.html",
    host: { style: "display: contents" },
})
export class CommunicationMenuComponent {
    readonly project = projectRequest;
    readonly navigation = readNavigationData();
}

@Component({
    selector: "nw-header",
    templateUrl: "./header.html",
    host: { style: "display: contents" },
})
export class HeaderComponent {
    readonly project = projectRequest;
    readonly navigation = readNavigationData();
}
