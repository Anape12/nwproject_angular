import { expect, test } from "@playwright/test";

const userId = process.env.E2E_USER_ID ?? "a0001";
const password = process.env.E2E_PASSWORD ?? "k1226";

test.describe("ログイン後のメニュー選択", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/Login");
    });

    test("認証失敗を表示し、ログインフォームに戻れる", async ({ page }) => {
        await page.locator("#userId").fill("");
        await page.getByRole("button", { name: "ログイン" }).click();

        await expect(page.getByRole("heading", { name: "ログインできませんでした" })).toBeVisible();
        await page.getByRole("link", { name: "ログイン画面に戻る" }).click();
        await expect(page.locator("#userId")).toBeVisible();
    });

    test("チャットメニューと業務メニューを選択できる", async ({ page }) => {
        await page.locator("#userId").fill(userId);
        await page.locator("#password").fill(password);
        await page.getByRole("button", { name: "ログイン" }).click();

        await expect(page).toHaveURL((url) => url.pathname === "/MenuSelect");
        await expect(page).toHaveTitle("利用メニューを選択｜NW Project");
        await expect(page.getByRole("heading", { name: "利用するメニューを選択" })).toBeVisible();
        await expect(page.getByRole("link", { name: /チャットメニュー/ })).toHaveAttribute("href", /[?&]_loginContext=/);

        await page.getByRole("link", { name: /チャットメニュー/ }).click();
        await expect(page).toHaveURL((url) => url.pathname === "/CommunicationMenu");
        await expect(page.getByRole("heading", { name: "コミュニケーション" })).toBeVisible();
        await expect(page.getByRole("link", { name: /チャット/ })).toHaveAttribute("target", "_blank");
        await expect(page.getByRole("link", { name: /スレッド/ })).toBeVisible();

        await page.getByRole("link", { name: /メニュー切替/ }).click();
        await expect(page).toHaveURL((url) => url.pathname === "/MenuSelect");

        await page.getByRole("link", { name: /業務メニュー/ }).click();
        await expect(page).toHaveURL((url) => url.pathname === "/BusinessMenu");
        await expect(page).toHaveTitle("ホーム｜NW Project");
        await expect(page.getByRole("heading", { name: /お疲れさまです/ })).toBeVisible();
        await expect(page.getByRole("navigation").getByRole("link", { name: "切替", exact: true })).toHaveAttribute(
            "href",
            /\/MenuSelect\?_loginContext=/,
        );
    });
});
