const storageKey = "nwproject.loginContext";
const parameterName = "_loginContext";
const headerName = "X-NW-Login-Context";

let windowContext = "";
let applicationPath = "";

export function initializeLoginContext(serverContext: string, contextPath: string): void {
    const currentUrl = new URL(location.href);
    const freshLogin = currentUrl.searchParams.get("loginFresh") === "1";

    if (freshLogin || !sessionStorage.getItem(storageKey)) {
        sessionStorage.setItem(storageKey, serverContext);
    }

    windowContext = sessionStorage.getItem(storageKey) || "";
    applicationPath = contextPath;
    if (!windowContext) return;

    if (freshLogin) {
        currentUrl.searchParams.delete("loginFresh");
        history.replaceState(null, "", currentUrl.pathname + currentUrl.search + currentUrl.hash);
    }

    decorate(document);
    new MutationObserver((mutations) => {
        for (const mutation of mutations) {
            for (const node of mutation.addedNodes) {
                if (node instanceof Element) decorate(node);
            }
        }
    }).observe(document.documentElement, { childList: true, subtree: true });
}

export async function requestWithLoginContext(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const requestUrl = input instanceof Request ? input.url : input;
    if (windowContext && sameOriginUrl(String(requestUrl))) {
        const headers = new Headers(init?.headers || (input instanceof Request ? input.headers : undefined));
        headers.set(headerName, windowContext);
        init = { ...init, headers };
    }
    const response = await fetch(input, init);
    if (windowContext && response.status === 409 && response.headers.get("X-NW-Window-Invalid") === "1") {
        location.replace(applicationPath + "/Login?windowInvalid=1");
    }
    return response;
}

function sameOriginUrl(value: string | null): URL | null {
    if (!value || /^(#|javascript:|mailto:)/.test(value)) return null;
    try {
        const url = new URL(value, location.href);
        return url.origin === location.origin ? url : null;
    } catch {
        return null;
    }
}

function decorate(root: Document | Element): void {
    if (root instanceof HTMLFormElement) decorateForm(root);
    if (root instanceof HTMLAnchorElement) decorateLink(root);
    root.querySelectorAll("form").forEach(decorateForm);
    root.querySelectorAll<HTMLAnchorElement>("a[href]").forEach(decorateLink);
}

function decorateForm(form: HTMLFormElement): void {
    let field = form.querySelector<HTMLInputElement>('input[name="' + parameterName + '"]');
    if (!field) {
        field = document.createElement("input");
        field.type = "hidden";
        field.name = parameterName;
        form.appendChild(field);
    }
    field.value = windowContext;
}

function decorateLink(link: HTMLAnchorElement): void {
    if (link.hasAttribute("download")) return;
    const url = sameOriginUrl(link.getAttribute("href"));
    if (!url) return;
    url.searchParams.set(parameterName, windowContext);
    link.href = url.toString();
}
