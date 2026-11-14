import { expect, test } from "@playwright/test";

test.describe("生年月日の暗号化保存", () => {
    test.skip(process.env.BIRTHDAY_E2E !== "true", "専用の暗号化テスト環境で実行します");

    test("本人の表示と変更、他人へのアクセス拒否", async ({ page }) => {
        await page.goto("/Login");
        await page.getByLabel("ユーザーID", { exact: true }).fill("privacyAlice");
        await page.getByLabel("パスワード", { exact: true }).fill("TemporaryTest123");
        await page.getByRole("button", { name: "ログイン", exact: true }).click();
        await expect(page).toHaveURL((url) => url.pathname === "/MenuSelect");
        await page.goto("/UserBirthday");
        await expect(page.getByRole("heading", { name: "生年月日の確認・変更" })).toBeVisible();
        await expect(page.locator('input[name="birthday"]')).toHaveValue("1996-04-13");
        await page.locator('input[name="birthday"]').fill("1996-04-14");
        const saved = page.waitForResponse((response) =>
            new URL(response.url()).pathname === "/UserBirthday" && response.request().method() === "POST",
        );
        await page.getByRole("button", { name: "保存する" }).click();
        expect((await saved).status()).toBe(302);
        await page.goto("/UserBirthday");
        await expect(page.locator('input[name="birthday"]')).toHaveValue("1996-04-14");

        const denied = page.waitForResponse((response) => {
            const url = new URL(response.url());
            return url.pathname === "/UserBirthday" && url.searchParams.get("format") === "json"
                && url.searchParams.get("userId") === "privacyBob";
        });
        await page.goto("/UserBirthday?userId=privacyBob");
        expect((await denied).status()).toBe(403);
        await expect(page.getByRole("alert")).toContainText("画面を読み込めませんでした");
        await expect(page.locator('input[name="birthday"]')).toHaveCount(0);
    });

    test("管理者は対象ユーザーを編集できる", async ({ page }) => {
        await page.goto("/Login");
        await page.getByLabel("ユーザーID", { exact: true }).fill("privacyAdmin");
        await page.getByLabel("パスワード", { exact: true }).fill("TemporaryTest123");
        await page.getByRole("button", { name: "ログイン", exact: true }).click();
        await expect(page).toHaveURL((url) => url.pathname === "/MenuSelect");
        await page.goto("/UserBirthday?userId=privacyBob");
        await expect(page.locator('input[name="birthday"]')).toHaveValue("1988-02-03");
        await page.locator('input[name="birthday"]').fill("1988-02-05");
        const saved = page.waitForResponse((response) =>
            new URL(response.url()).pathname === "/UserBirthday" && response.request().method() === "POST",
        );
        await page.getByRole("button", { name: "保存する" }).click();
        expect((await saved).status()).toBe(302);
        await page.goto("/UserBirthday?userId=privacyBob");
        await expect(page.locator('input[name="birthday"]')).toHaveValue("1988-02-05");
        // A valid login session alone must not make a POST without a CSRF token succeed.
        const loginContext = await page.evaluate(() => sessionStorage.getItem("nwproject.loginContext"));
        const response = await page.request.post("/UserBirthday", {
            form: { userId: "privacyBob", birthday: "1988-02-04" },
            headers: { "X-NW-Login-Context": loginContext ?? "" },
            maxRedirects: 0,
        });
        expect(response.status()).toBe(403);
        await page.reload();
        await expect(page.locator('input[name="birthday"]')).toHaveValue("1988-02-05");
    });
});
