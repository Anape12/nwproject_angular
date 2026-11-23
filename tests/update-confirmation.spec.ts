import { expect, Page, test } from "@playwright/test";
import { readFileSync } from "node:fs";

const origin = "http://update-confirmation.test";

async function mount(page: Page, selector: string, endpoint: string, data: Record<string, unknown> = {}, failureStatus = 400) {
    const posts: URLSearchParams[] = [];
    await page.route(`${origin}/**`, async (route) => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname === "/js/angular/main.js" || url.pathname === "/js/angular/styles.css") {
            const name = url.pathname.endsWith("main.js") ? "main.js" : "styles.css";
            await route.fulfill({
                body: readFileSync(`target/angular/browser/${name}`),
                contentType: name.endsWith("js") ? "application/javascript" : "text/css",
            });
        } else if (request.method() === "POST") {
            posts.push(new URLSearchParams(request.postData() ?? ""));
            if (selector === "nw-change-password" || selector === "nw-user-registration" || selector === "nw-login") {
                await route.fulfill({
                    status: selector === "nw-login" ? 401 : failureStatus,
                    json: { message: "テスト応答" },
                });
            } else {
                await new Promise((resolve) => setTimeout(resolve, 700));
                await route.fulfill({ contentType: "text/html", body: '<meta charset="utf-8"><h1>更新完了</h1>' });
            }
        } else if (url.searchParams.get("format") === "json") {
            await route.fulfill({ json: { contextPath: "", loginContext: "", csrfToken: "test-csrf", ...data } });
        } else {
            await route.fulfill({
                contentType: "text/html",
                body: `<meta charset="utf-8"><link rel="stylesheet" href="/js/angular/styles.css">
                    <${selector} ${selector === "nw-login" ? "" : 'data-static-page="current"'}></${selector}>
                    <script type="module" src="/js/angular/main.js"></script>`,
            });
        }
    });
    await page.goto(`${origin}/${endpoint}`);
    return posts;
}

const confirmation = (page: Page) => page.getByRole("dialog", { name: "更新の確認" });

test("ログインには確認を表示せず認証する", async ({ page }) => {
    const posts = await mount(page, "nw-login", "Login");
    await page.getByLabel("ユーザーID", { exact: true }).fill("unknownUser");
    await page.getByLabel("パスワード", { exact: true }).fill("InvalidTest123");
    await page.getByRole("button", { name: "ログイン", exact: true }).click();
    await expect(page.getByRole("heading", { name: "ログインできませんでした" })).toBeVisible();
    expect(posts).toHaveLength(1);
    await expect(page.getByRole("dialog")).toHaveCount(0);
});

for (const { registration, status } of [
    { registration: false, status: 400 },
    { registration: true, status: 400 },
    { registration: true, status: 503 },
]) {
    test(`${registration ? "ユーザー登録" : "パスワード変更"}は確認後だけAPIを呼び、キャンセル時は入力を保持する（${status}応答）`, async ({ page }) => {
        const posts = await mount(page, registration ? "nw-user-registration" : "nw-change-password",
            registration ? "UserRegistration" : "ChangePassword", {
                currentDate: "2026-11-23", permissions: [{ id: "2", label: "一般ユーザー" }],
            }, status);
        const button = page.getByRole("button", { name: registration ? "ユーザーを登録" : "パスワードを変更", exact: true });
        await button.click();
        await expect(confirmation(page)).toBeHidden();
        const values = registration ? {
            lastName: "テスト", firstName: "太郎", birthday: "1996-04-13", userId: "testUser",
            passwordExpiration: "2027-11-23", password: "TemporaryTest123", passwordConfirmation: "TemporaryTest123",
        } : { currentPassword: "CurrentTest123", newPassword: "NewPassword123", confirmation: "NewPassword123" };
        for (const [name, value] of Object.entries(values)) await page.locator(`input[name="${name}"]`).fill(value);
        if (registration) await page.locator('select[name="permission"]').selectOption("2");
        await button.click();
        await expect(confirmation(page)).toBeVisible();
        await expect(page.getByRole("progressbar")).toBeHidden();
        expect(posts).toHaveLength(0);
        await confirmation(page).getByRole("button", { name: "キャンセル" }).click();
        await expect(confirmation(page)).toBeHidden();
        await expect(button).toBeFocused();
        for (const [name, value] of Object.entries(values)) await expect(page.locator(`input[name="${name}"]`)).toHaveValue(value);
        await button.click();
        await page.keyboard.press("Escape");
        await expect(confirmation(page)).toBeHidden();
        expect(posts).toHaveLength(0);
        await button.click();
        await confirmation(page).getByRole("button", { name: "実行する" }).click();
        await expect(page.getByRole("alert")).toContainText("テスト応答");
        expect(posts).toHaveLength(1);
        for (const [name, value] of Object.entries(values)) expect(posts[0].get(name)).toBe(value);
        expect(posts[0].get("csrfToken")).toBe("test-csrf");
    });
}

test("生年月日の保存はキャンセル可能で、OK後に通常のフォーム送信と進行表示を行う", async ({ page }) => {
    const posts = await mount(page, "nw-user-birthday", "UserBirthday", {
        targetUserId: "testUser", birthday: "1996-04-13",
    });
    await page.locator('input[name="birthday"]').fill("1996-04-14");
    await page.getByRole("button", { name: "保存する" }).click();
    await expect(confirmation(page)).toBeVisible();
    await expect(page.getByRole("progressbar")).toBeHidden();
    expect(posts).toHaveLength(0);
    await confirmation(page).getByRole("button", { name: "キャンセル" }).click();
    await expect(page.locator('input[name="birthday"]')).toHaveValue("1996-04-14");
    await page.getByRole("button", { name: "保存する" }).click();
    // Read during navigation inside the existing document; locator assertions wait for navigation.
    const loading = await page.evaluate(async () => {
        document.querySelector<HTMLButtonElement>("dialog[open] .confirm-button")!.click();
        await new Promise((resolve) => setTimeout(resolve, 150));
        return document.querySelector<HTMLElement>(".nw-progress")?.hidden === false;
    });
    expect(loading).toBe(true);
    await expect(page.getByRole("heading", { name: "更新完了" })).toBeVisible();
    expect(posts).toHaveLength(1);
    expect(posts[0].get("birthday")).toBe("1996-04-14");
    expect(posts[0].get("userId")).toBe("testUser");
    expect(posts[0].get("csrfToken")).toBe("test-csrf");
});

test("承認・差戻しでは押したボタンのdecisionを保持する", async ({ page }) => {
    const posts = await mount(page, "nw-report-approval", "ReportApproval", {
        applications: [{ id: "123", status: "SUBMITTED", title: "テスト申請" }],
    });
    await page.locator('input[name="approvalIds"]').check();
    await page.getByRole("button", { name: "一括差戻し" }).click();
    await expect(confirmation(page)).toContainText("一括差戻し");
    expect(posts).toHaveLength(0);
    await confirmation(page).getByRole("button", { name: "キャンセル" }).click();
    await page.getByRole("button", { name: "一括承認" }).click();
    await expect(confirmation(page)).toContainText("一括承認");
    await confirmation(page).getByRole("button", { name: "実行する" }).click();
    await expect(page.getByRole("heading", { name: "更新完了" })).toBeVisible();
    expect(posts).toHaveLength(1);
    expect(posts[0].get("decision")).toBe("APPROVED");
    expect(posts[0].getAll("approvalIds")).toEqual(["123"]);
    expect(posts[0].get("csrfToken")).toBe("test-csrf");
});
