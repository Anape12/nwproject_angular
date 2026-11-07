import { requestWithLoginContext } from "./login-context";
export async function loadPageData<T>(contextPath: string, path: string, query: URLSearchParams): Promise<T | null> {
    const url = new URL(contextPath + path, location.origin);
    url.search = query.toString();
    url.searchParams.set("format", "json");

    const response = await requestWithLoginContext(url, { mode: "same-origin" });
    if (response.redirected) {
        location.assign(response.url);
        return null;
    }
    if (!response.ok) {
        throw new Error("Page data request failed");
    }
    return response.json();
}
