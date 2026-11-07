import { Component, ElementRef, inject } from "@angular/core";
import { loadPageData } from "../shared/page-data";

interface MenuData {
    [key: string]: string | boolean | null;
    contextPath: string;
    firstName: string | null;
    lastName: string | null;
    loginContext: string;
}

export async function prepareStaticPage(host: HTMLElement): Promise<boolean> {
    const endpoint = host.dataset["staticPage"];
    const query = new URLSearchParams(location.search);
    if (endpoint) {
        for (const key of [...query.keys()]) {
            if (key !== "_loginContext" && key !== "loginFresh") query.delete(key);
        }
    }
    const path = endpoint ? new URL(endpoint, location.href).pathname : location.pathname;
    const data = await loadPageData<MenuData>("", path, query);
    if (!data) return false;
    const { loginContext, contextPath, firstName, lastName, ...pageData } = data;
    const navigation = { contextPath, firstName: firstName ?? "", lastName: lastName ?? "" };
    Object.assign(host.dataset, navigation);
    setPageData(host, pageData);
    const header = document.querySelector<HTMLElement>("nw-header");
    if (header) Object.assign(header.dataset, navigation, { loggedIn: "true" });

    Object.assign(window, { NW_SERVER_LOGIN_CONTEXT: loginContext, NW_CONTEXT_PATH: contextPath });
    await new Promise<void>((resolve, reject) => {
        const script = document.createElement("script");
        script.src = contextPath + "/js/login-context.js?v=20261107-2";
        script.onload = () => resolve();
        script.onerror = () => reject(new Error("Login context script failed to load"));
        document.head.appendChild(script);
    });
    if (host.hasAttribute("data-page-json")) {
        const content = await loadPageData<Record<string, unknown>>(
            "", location.pathname, new URLSearchParams(location.search),
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
