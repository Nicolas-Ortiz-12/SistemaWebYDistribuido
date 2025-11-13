// services/auth.js
const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

async function fetchJson(url, opts = {}) {
    const res = await fetch(url, opts);
    if (!res.ok) {
        let text = await res.text().catch(() => res.statusText);
        try { const j = JSON.parse(text); text = j.message || j.error || text; } catch { }
        throw new Error(text || `HTTP ${res.status}`);
    }
    return res.json();
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
    const token = localStorage.getItem("access_token");
    const type = localStorage.getItem("token_type") || "Bearer";
    return token ? { Authorization: `${type} ${token}` } : {};
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
        const rt = localStorage.getItem("refresh_token");
        if (rt) {
            try {
                const r = await refreshToken(rt);
                // guarda los nuevos valores
                if (r?.access_token) localStorage.setItem("access_token", r.access_token);
                if (r?.token_type) localStorage.setItem("token_type", r.token_type);
                if (r?.expires_in_minutes) localStorage.setItem("expires_in_minutes", r.expires_in_minutes);

                // reintenta con el nuevo token
                const retryHeaders = {
                    ...opts.headers,
                    ...authHeader(),
                    Accept: "application/json",
                };
                res = await fetch(url, { ...opts, headers: retryHeaders });
            } catch {
                // refresh falló: limpia y lanza 401
                localStorage.removeItem("access_token");
                localStorage.removeItem("refresh_token");
                localStorage.removeItem("token_type");
            }
        }
    }

    if (!res.ok) {
        let text = await res.text().catch(() => res.statusText);
        try { const j = JSON.parse(text); text = j.message || j.error || text; } catch { }
        throw new Error(text || `HTTP ${res.status}`);
    }

    return res.json();
}

export default { login, refreshToken, authHeader, fetchWithAuth };
