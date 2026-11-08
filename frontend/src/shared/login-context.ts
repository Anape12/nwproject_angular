const storageKey = "nwproject.loginContext";
const parameterName = "_loginContext";

let windowContext = "";

export function initializeLoginContext(serverContext: string): void {
    const currentUrl = new URL(location.href);
    const freshLogin = currentUrl.searchParams.get("loginFresh") === "1";

    if (freshLogin || !sessionStorage.getItem(storageKey)) {
        sessionStorage.setItem(storageKey, serverContext);
    }

    windowContext = sessionStorage.getItem(storageKey) || "";
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

export function getLoginContext(): string {
    return windowContext;
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
