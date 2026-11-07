(function () {
    "use strict";

    if (window.__nwLoginContextInitialized) return;
    window.__nwLoginContextInitialized = true;

    const storageKey = "nwproject.loginContext";
    const parameterName = "_loginContext";
    const headerName = "X-NW-Login-Context";
    const serverContext = window.NW_SERVER_LOGIN_CONTEXT || "";
    const currentUrl = new URL(window.location.href);
    const freshLogin = currentUrl.searchParams.get("loginFresh") === "1";

    if (freshLogin || !sessionStorage.getItem(storageKey)) {
        sessionStorage.setItem(storageKey, serverContext);
    }

    const windowContext = sessionStorage.getItem(storageKey) || "";
    if (!windowContext) return;

    if (freshLogin) {
        currentUrl.searchParams.delete("loginFresh");
        history.replaceState(null, "", currentUrl.pathname + currentUrl.search + currentUrl.hash);
    }

    function sameApplicationUrl(value) {
        if (!value) return null;
        const text = String(value);
        if (text.startsWith("#") || text.startsWith("javascript:") || text.startsWith("mailto:")) return null;
        try {
            const url = new URL(text, window.location.href);
            return url.origin === window.location.origin ? url : null;
        } catch (_error) {
            return null;
        }
    }

    function decorateForm(form) {
        if (!(form instanceof HTMLFormElement)) return;
        let field = form.querySelector('input[name="' + parameterName + '"]');
        if (!field) {
            field = document.createElement("input");
            field.type = "hidden";
            field.name = parameterName;
            form.appendChild(field);
        }
        field.value = windowContext;
    }

    function decorateLink(link) {
        if (!(link instanceof HTMLAnchorElement) || link.hasAttribute("download")) return;
        const url = sameApplicationUrl(link.getAttribute("href"));
        if (!url) return;
        url.searchParams.set(parameterName, windowContext);
        link.href = url.toString();
    }

    function decorate(root) {
        if (root instanceof HTMLFormElement) decorateForm(root);
        if (root instanceof HTMLAnchorElement) decorateLink(root);
        if (!root.querySelectorAll) return;
        root.querySelectorAll("form").forEach(decorateForm);
        root.querySelectorAll("a[href]").forEach(decorateLink);
    }

    function staleWindow() {
        window.location.replace((window.NW_CONTEXT_PATH || "") + "/Login?windowInvalid=1");
    }

    const originalFetch = window.fetch;
    if (originalFetch) {
        window.fetch = function (input, init) {
            const requestUrl = typeof input === "string" || input instanceof URL ? input : input.url;
            const url = sameApplicationUrl(requestUrl);
            const options = Object.assign({}, init || {});
            if (url) {
                const headers = new Headers(options.headers || (input instanceof Request ? input.headers : undefined));
                headers.set(headerName, windowContext);
                options.headers = headers;
            }
            return originalFetch.call(this, input, options).then(function (response) {
                if (response.status === 409 && response.headers.get("X-NW-Window-Invalid") === "1") staleWindow();
                return response;
            });
        };
    }

    const originalOpen = XMLHttpRequest.prototype.open;
    const originalSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function (method, url) {
        this.__nwSameApplication = Boolean(sameApplicationUrl(url));
        return originalOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function () {
        if (this.__nwSameApplication) this.setRequestHeader(headerName, windowContext);
        this.addEventListener("load", function () {
            if (this.status === 409 && this.getResponseHeader("X-NW-Window-Invalid") === "1") staleWindow();
        });
        return originalSend.apply(this, arguments);
    };

    function startDecorating() {
        decorate(document);
        new MutationObserver(function (mutations) {
            mutations.forEach(function (mutation) {
                mutation.addedNodes.forEach(function (node) {
                    if (node.nodeType === Node.ELEMENT_NODE) decorate(node);
                });
            });
        }).observe(document.documentElement, { childList: true, subtree: true });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", startDecorating, { once: true });
    } else {
        startDecorating();
    }
})();
