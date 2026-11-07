import { expect, test } from "@playwright/test";
import { readFileSync } from "node:fs";
import path from "node:path";
import ts from "typescript";

const origin = "http://progress.test";
const modulePath = "/server-progress.mjs";
const script = ts.transpileModule(readFileSync("frontend/src/shared/server-progress.ts", "utf8"), {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext },
}).outputText;
const stylePath = path.resolve("frontend/src/shared/server-progress.css");
const initialize = `<script type="module">
    import { initializeServerProgress } from "${modulePath}";
    window.progressFixture = initializeServerProgress();
</script>`;

declare global {
    interface Window {
        progressFixture: ReturnType<typeof import("../frontend/src/shared/server-progress").initializeServerProgress>;
    }
}

test.beforeEach(async ({ page, context }) => {
    await context.route(`${origin}/**`, async (route) => {
        if (new URL(route.request().url()).pathname === modulePath) {
            await route.fulfill({ body: script, contentType: "application/javascript" });
            return;
        }
        if (["/search", "/next"].includes(new URL(route.request().url()).pathname)) {
            await new Promise((resolve) => setTimeout(resolve, 900));
            await route.fulfill({
                contentType: "text/html",
                body: '<meta charset="utf-8"><h1>移動先</h1>' + initialize,
            });
            return;
        }
        await route.fulfill({
            contentType: "text/html",
            body: '<meta charset="utf-8"><form action="/search" method="get"><button type="submit">検索</button></form><form action="/search" method="get" target="_blank"><button type="submit">別タブ検索</button></form><a href="/next">次の画面</a><a href="/next" target="_blank" rel="noopener">別タブ</a><a href="/QiitaGuide" data-nw-progress-ignore>Qiita記事を聴く</a>' + initialize,
        });
    });
    await page.goto(`${origin}/`);
    await page.addStyleTag({ path: stylePath });
});

test("検索の待機中に表示し、画面遷移後は残さない", async ({ page }) => {
    const visibleDuringRequest = await page.evaluate(async () => {
        document.querySelector("button")?.click();
        await new Promise((resolve) => setTimeout(resolve, 250));
        return document.querySelector<HTMLElement>(".nw-progress")?.hidden === false;
    });
    expect(visibleDuringRequest).toBe(true);
    await expect(page.getByRole("heading", { name: "移動先" })).toBeVisible();
    await expect(page.locator(".nw-progress")).toHaveCount(0);
});

test("同一タブの画面遷移には表示する", async ({ page }) => {
    const visibleDuringRequest = await page.evaluate(async () => {
        document.querySelector<HTMLAnchorElement>('a[href="/next"]')?.click();
        await new Promise((resolve) => setTimeout(resolve, 250));
        return document.querySelector<HTMLElement>(".nw-progress")?.hidden === false;
    });
    expect(visibleDuringRequest).toBe(true);
    await expect(page.getByRole("heading", { name: "移動先" })).toBeVisible();
});

test("送信がキャンセルされた場合は進行表示を消す", async ({ page }) => {
    await page.evaluate(() => {
        document.querySelector("form")?.addEventListener("submit", (event) => event.preventDefault());
    });
    await page.getByRole("button", { name: "検索", exact: true }).click();
    await page.waitForTimeout(250);
    await expect(page.locator(".nw-progress")).toBeHidden();
    await expect(page.locator("body")).not.toHaveAttribute("aria-busy");
    await expect(page).toHaveURL(`${origin}/`);
});

test("画面をマスクし、中央に進行表示を置いて操作を防ぐ", async ({ page }) => {
    await page.getByRole("button", { name: "検索", exact: true }).focus();
    await page.evaluate(() => window.progressFixture.start("処理中…"));
    await expect(page.locator(".nw-progress")).toBeVisible();
    await expect(page.locator("body")).toHaveAttribute("aria-busy", "true");
    const layout = await page.evaluate(() => {
        const mask = document.querySelector<HTMLElement>(".nw-progress")!;
        const panel = document.querySelector<HTMLElement>(".nw-progress-panel")!;
        const rect = panel.getBoundingClientRect();
        return {
            fullScreen:
                mask.getBoundingClientRect().width === innerWidth &&
                mask.getBoundingClientRect().height === innerHeight,
            centerX: Math.abs(rect.left + rect.width / 2 - innerWidth / 2),
            centerY: Math.abs(rect.top + rect.height / 2 - innerHeight / 2),
            pointerEvents: getComputedStyle(mask).pointerEvents,
        };
    });
    expect(layout.fullScreen).toBe(true);
    expect(layout.centerX).toBeLessThan(1);
    expect(layout.centerY).toBeLessThan(1);
    expect(layout.pointerEvents).toBe("auto");
    await page.keyboard.press("Enter");
    await page.waitForTimeout(250);
    await expect(page).toHaveURL(`${origin}/`);
    await page.evaluate(() => window.progressFixture.stop());
    await expect(page.locator(".nw-progress")).toBeHidden();
    await expect(page.locator("body")).not.toHaveAttribute("aria-busy");
    await page.getByRole("button", { name: "検索", exact: true }).click();
    await expect(page.getByRole("heading", { name: "移動先" })).toBeVisible();
});

test("別タブの画面遷移が終わると元画面の進行表示を消す", async ({ page }) => {
    const popupPromise = page.waitForEvent("popup");
    const clickPromise = page.getByRole("link", { name: "別タブ" }).click();
    await expect(page.locator(".nw-progress")).toBeVisible();
    const popup = await popupPromise;
    await clickPromise;
    await expect(popup.getByRole("heading", { name: "移動先" })).toBeVisible();
    await expect(page.locator(".nw-progress")).toBeHidden();
    await expect(popup).toHaveURL(`${origin}/next`);
});

test("別タブへの検索送信も完了後に進行表示を消す", async ({ page }) => {
    const popupPromise = page.waitForEvent("popup");
    const clickPromise = page.getByRole("button", { name: "別タブ検索" }).click();
    await expect(page.locator(".nw-progress")).toBeVisible();
    const popup = await popupPromise;
    await clickPromise;
    await expect(popup.getByRole("heading", { name: "移動先" })).toBeVisible();
    await expect(page.locator(".nw-progress")).toBeHidden();
    await expect(popup).toHaveURL(`${origin}/search`);
});

test("業務メニューの base target=_blank でも進行表示する", async ({ page }) => {
    await page.evaluate(() => {
        const base = document.createElement("base");
        base.target = "_blank";
        document.head.appendChild(base);
    });
    const popupPromise = page.waitForEvent("popup");
    const clickPromise = page.getByRole("link", { name: "次の画面" }).click();
    await expect(page.locator(".nw-progress")).toBeVisible();
    const popup = await popupPromise;
    await clickPromise;
    await expect(popup.getByRole("heading", { name: "移動先" })).toBeVisible();
    await expect(page.locator(".nw-progress")).toBeHidden();
});

test("外部アプリへ転送するリンクはメニューをマスクしない", async ({ page }) => {
    await page.evaluate(() => {
        const base = document.createElement("base");
        base.target = "_blank";
        document.head.appendChild(base);
    });
    const popupPromise = page.waitForEvent("popup");
    await page.getByRole("link", { name: "Qiita記事を聴く" }).click();
    const popup = await popupPromise;
    expect(popup.isClosed()).toBe(false);
    await expect(page.locator(".nw-progress")).toHaveCount(0);
});
