import { API_BASE } from "./api";
import {
    clearAuthTokens,
    getAuthHeader,
    readAuthValue,
    saveAuthTokens,
    updateAccessToken,
} from "./authStorage";

async function fetchJson(url, opts = {}) {
    const res = await fetch(url, opts);
    if (!res.ok) {
        let text = await res.text().catch(() => res.statusText);
        try { const j = JSON.parse(text); text = j.message || j.error || text; } catch { }
        throw new Error(text || `HTTP ${res.status}`);
    }
    if (res.status === 204) return null;
    const text = await res.text().catch(() => "");
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

export async function login({ username, password }) {
    const url = `${API_BASE}/auth/login`;
    return fetchJson(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
    });
}

export async function refreshToken(refreshToken) {
    const url = `${API_BASE}/auth/refresh`;
    return fetchJson(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refresh_token: refreshToken })
    });
}

export function authHeader() {
    return getAuthHeader();
}

/**
 * fetch con Authorization automático y refresh on 401 (un solo reintento)
 */
export async function fetchWithAuth(url, opts = {}) {
    const mergedHeaders = {
        Accept: "application/json",
        ...opts.headers,
        ...authHeader(),
    };

    let res = await fetch(url, { ...opts, headers: mergedHeaders });

    if (res.status === 401) {
        const rt = readAuthValue("refresh_token");
        if (rt) {
            try {
                const r = await refreshToken(rt);
                updateAccessToken(r);

                // reintenta con el nuevo token
                const retryHeaders = {
                    ...opts.headers,
                    ...authHeader(),
                    Accept: "application/json",
                };
                res = await fetch(url, { ...opts, headers: retryHeaders });
            } catch {
                // refresh falló: limpia y lanza 401
                clearAuthTokens();
            }
        }
    }

    if (!res.ok) {
        let text = await res.text().catch(() => res.statusText);
        try { const j = JSON.parse(text); text = j.message || j.error || text; } catch { }
        throw new Error(text || `HTTP ${res.status}`);
    }

    if (res.status === 204) return null;
    const text = await res.text().catch(() => "");
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

export default { login, refreshToken, authHeader, fetchWithAuth };
