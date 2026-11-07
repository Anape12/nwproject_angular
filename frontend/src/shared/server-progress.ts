export function initializeServerProgress() {
    let currentId = 0;
    let safetyTimer: number | undefined;
    let indicator: HTMLDivElement | undefined;
    let active = false;
    let previousBusy: string | null = null;
    const progressParameter = "_nwProgressId";
    const progressChannel = typeof BroadcastChannel === "function" ? new BroadcastChannel("nwproject.progress") : null;
    const pendingTabs = new Map<string, () => void>();

    if (progressChannel) {
        progressChannel.addEventListener("message", function (event) {
            const done = pendingTabs.get(event.data?.id);
            if (event.data?.type !== "ready" || !done) return;
            pendingTabs.delete(event.data.id);
            done();
        });
        const incomingId = new URL(location.href).searchParams.get(progressParameter);
        if (incomingId) {
            const notifyReady = () => {
                progressChannel.postMessage({ type: "ready", id: incomingId });
                const cleanUrl = new URL(location.href);
                cleanUrl.searchParams.delete(progressParameter);
                history.replaceState(history.state, "", cleanUrl.pathname + cleanUrl.search + cleanUrl.hash);
            };
            if (document.readyState === "complete") notifyReady();
            else window.addEventListener("load", notifyReady, { once: true });
        }
    }

    function ensureIndicator() {
        if (indicator || !document.body) return indicator;
        indicator = document.createElement("div");
        indicator.className = "nw-progress";
        indicator.hidden = true;
        indicator.innerHTML =
            '<div class="nw-progress-panel">' +
            '<div class="nw-progress-track" role="progressbar" aria-label="処理中"></div>' +
            '<div class="nw-progress-message" role="status" aria-live="polite"></div>' +
            "</div>";
        document.body.appendChild(indicator);
        return indicator;
    }

    function stop(id?: number) {
        if (id != null && id !== currentId) return;
        clearTimeout(safetyTimer);
        if (indicator) indicator.hidden = true;
        if (active && document.body) {
            if (previousBusy === null) document.body.removeAttribute("aria-busy");
            else document.body.setAttribute("aria-busy", previousBusy);
        }
        active = false;
    }

    function start(message: string, timeoutMs = 120000) {
        const id = ++currentId;
        stop();
        const element = ensureIndicator();
        if (element) {
            previousBusy = document.body.getAttribute("aria-busy");
            document.body.setAttribute("aria-busy", "true");
            const label = element.querySelector(".nw-progress-message");
            if (label) label.textContent = message;
            element.hidden = false;
            active = true;
        }
        // If a navigation is cancelled by the browser, do not leave the indicator stuck forever.
        safetyTimer = window.setTimeout(function () {
            stop(id);
        }, timeoutMs);
        return function () {
            stop(id);
        };
    }

    function ownTab(target: string) {
        const base = document.querySelector<HTMLBaseElement>("base[target]");
        const effective = (target || (base && base.target) || "_self").toLowerCase();
        return effective === "_self" || effective === "_top" || effective === "_parent";
    }

    function newTab(target: string) {
        const base = document.querySelector<HTMLBaseElement>("base[target]");
        const effective = (target || (base && base.target) || "_self").toLowerCase();
        return effective === "_blank" || !effective.startsWith("_");
    }

    function trackNewTab() {
        const id = Array.from(crypto.getRandomValues(new Uint32Array(4)), (value) => value.toString(16)).join("");
        const done = start("別タブを読み込んでいます…", 30000);
        const cleanup = setTimeout(function () {
            pendingTabs.delete(id);
        }, 30000);
        const tracking = {
            id: id,
            finish: function () {
                clearTimeout(cleanup);
                pendingTabs.delete(id);
                done();
            },
        };
        pendingTabs.set(id, tracking.finish);
        return tracking;
    }

    function navigable(urlText: string | null) {
        if (!urlText || urlText.startsWith("#")) return false;
        try {
            const url = new URL(urlText, location.href);
            if (url.origin !== location.origin || !/^https?:$/.test(url.protocol)) return false;
            if (url.pathname === location.pathname && url.search === location.search && url.hash) return false;
            // Downloads do not replace the page, so there is no page load that would clear the indicator.
            if (/\/(?:Attachment|CalendarExchange)$/.test(url.pathname)) return false;
            return true;
        } catch (_error) {
            return false;
        }
    }

    function blockInteraction(event: Event) {
        if (!active) return;
        event.preventDefault();
        event.stopImmediatePropagation();
    }

    document.addEventListener("click", blockInteraction, true);
    document.addEventListener("submit", blockInteraction, true);
    document.addEventListener("keydown", blockInteraction, true);

    document.addEventListener(
        "click",
        function (event) {
            const link = event.target instanceof Element ? event.target.closest<HTMLAnchorElement>("a[href]") : null;
            if (
                !link ||
                event.button !== 0 ||
                event.metaKey ||
                event.ctrlKey ||
                event.shiftKey ||
                event.altKey ||
                link.hasAttribute("download") ||
                link.dataset.nwProgressIgnore !== undefined ||
                !navigable(link.getAttribute("href"))
            )
                return;
            const separateTab = newTab(link.target);
            if (!separateTab && !ownTab(link.target)) return;
            let done: () => void;
            let restore: (() => void) | undefined;
            if (separateTab && progressChannel) {
                const originalHref = link.href;
                const tracking = trackNewTab();
                const url = new URL(originalHref);
                url.searchParams.set(progressParameter, tracking.id);
                link.href = url.href;
                done = tracking.finish;
                restore = function () {
                    link.href = originalHref;
                };
            } else {
                done = start("画面を読み込んでいます…", separateTab ? 30000 : undefined);
            }
            setTimeout(function () {
                restore?.();
                if (event.defaultPrevented) done();
            }, 0);
        },
        true
    );

    document.addEventListener(
        "submit",
        function (event) {
            const form = event.target;
            if (!(form instanceof HTMLFormElement)) return;
            const submitter = event.submitter;
            const action = submitter?.getAttribute("formaction") || form.getAttribute("action") || location.href;
            const target = submitter?.getAttribute("formtarget") || form.target;
            const method = submitter?.getAttribute("formmethod") || form.method || "get";
            if (form.dataset.nwProgressIgnore !== undefined || !navigable(action))
                return;
            const separateTab = newTab(target);
            if (!separateTab && !ownTab(target)) return;
            const value = submitter instanceof HTMLInputElement || submitter instanceof HTMLButtonElement ? submitter.value : "";
            const text = (submitter?.textContent || value).trim();
            const message =
                form.dataset.nwProgressMessage ||
                (/検索|絞り込み/.test(text)
                    ? "検索中…"
                    : /ログイン/.test(text)
                    ? "ログイン中…"
                    : /登録|保存|更新|申請|承認|削除|送信|作成/.test(text)
                    ? "処理中…"
                    : "読み込み中…");
            let done: () => void;
            let field: HTMLInputElement | undefined;
            if (separateTab && progressChannel && method.toLowerCase() === "get") {
                const tracking = trackNewTab();
                field = document.createElement("input");
                field.type = "hidden";
                field.name = progressParameter;
                field.value = tracking.id;
                form.appendChild(field);
                done = tracking.finish;
            } else {
                done = start(message, separateTab ? 30000 : undefined);
            }
            setTimeout(function () {
                if (field) field.remove();
                if (event.defaultPrevented) done();
            }, 0);
        },
        true
    );

    window.addEventListener("pagehide", function () {
        stop();
    });
    window.addEventListener("pageshow", function () {
        stop();
    });

    return { start, stop };
}
