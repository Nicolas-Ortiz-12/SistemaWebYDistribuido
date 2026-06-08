export const API_BASE = (import.meta.env.VITE_API_BASE ?? "/api").replace(/\/+$/, "");

export function apiUrl(path = "") {
    if (!path) return API_BASE;
    return `${API_BASE}${path.startsWith("/") ? "" : "/"}${path}`;
}
