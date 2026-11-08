import { getLoginContext } from "../shared/login-context";
import { paths } from "./api-path";

export type ProjectRoute = keyof typeof paths | "current";
type Query = URLSearchParams | Record<string, string | number>;
type RequestOptions = RequestInit & { query?: Query; json?: boolean };

function searchParams(query?: Query): URLSearchParams {
    return new URLSearchParams(query instanceof URLSearchParams
        ? query
        : Object.entries(query ?? {}).map(([key, value]) => [key, String(value)]));
}

class ProjectRequest {
    private contextPath: string | undefined;

    setContextPath(contextPath: string): void {
        this.contextPath = contextPath;
    }

    url(route: ProjectRoute, query?: Query): string {
        const contextPath = this.contextPath ?? location.pathname.slice(0, location.pathname.lastIndexOf("/"));
        const pathname = route === "current" ? location.pathname : contextPath + paths[route];
        const params = query instanceof URLSearchParams ? query : searchParams(query);
        return params.size ? pathname + "?" + params.toString() : pathname;
    }

    async request(route: ProjectRoute, { query, json, ...init }: RequestOptions = {}): Promise<Response> {
        const params = searchParams(query);
        if (json) params.set("format", "json");

        const loginContext = getLoginContext();
        const headers = new Headers(init.headers);
        if (loginContext) headers.set("X-NW-Login-Context", loginContext);
        const response = await fetch(this.url(route, params), { ...init, headers, mode: "same-origin" });
        if (loginContext && response.status === 409 && response.headers.get("X-NW-Window-Invalid") === "1") {
            location.replace(this.url("login", { windowInvalid: 1 }));
        }
        return response;
    }

    async loadPageData<T>(route: ProjectRoute, query?: Query): Promise<T | null> {
        const response = await this.request(route, { query, json: true });
        if (response.redirected) {
            location.assign(response.url);
            return null;
        }
        if (!response.ok) throw new Error("Page data request failed");
        return response.json();
    }
}

export const projectRequest = new ProjectRequest();
