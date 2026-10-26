const TOKEN_KEYS = [
    "access_token",
    "refresh_token",
    "token_type",
    "expires_in_minutes",
    "username",
];

function getStorageByRememberFlag() {
    return localStorage.getItem("remember_me") === "true"
        ? localStorage
        : sessionStorage;
}

export function getAuthStore() {
    if (sessionStorage.getItem("access_token") || sessionStorage.getItem("refresh_token")) {
        return sessionStorage;
    }
    if (localStorage.getItem("access_token") || localStorage.getItem("refresh_token")) {
        return localStorage;
    }
    return getStorageByRememberFlag();
}

export function readAuthValue(key) {
    return sessionStorage.getItem(key) ?? localStorage.getItem(key) ?? "";
}

export function getAuthHeader() {
    const token = readAuthValue("access_token");
    const type = readAuthValue("token_type") || "Bearer";
    return token ? { Authorization: `${type} ${token}` } : {};
}

export function saveAuthTokens(data, { remember = false, username = "" } = {}) {
    clearAuthTokens();
    const store = remember ? localStorage : sessionStorage;

    if (data?.access_token) store.setItem("access_token", data.access_token);
    if (data?.refresh_token) store.setItem("refresh_token", data.refresh_token);
    if (data?.token_type) store.setItem("token_type", data.token_type);
    if (data?.expires_in_minutes !== undefined) {
        store.setItem("expires_in_minutes", String(data.expires_in_minutes));
    }
    if (username) {
        store.setItem("username", username);
    }

    localStorage.setItem("remember_me", remember ? "true" : "false");
}

export function updateAccessToken(data) {
    const store = getAuthStore();
    if (data?.access_token) store.setItem("access_token", data.access_token);
    if (data?.token_type) store.setItem("token_type", data.token_type);
    if (data?.expires_in_minutes !== undefined) {
        store.setItem("expires_in_minutes", String(data.expires_in_minutes));
    }
}

export function clearAuthTokens() {
    for (const key of TOKEN_KEYS) {
        localStorage.removeItem(key);
        sessionStorage.removeItem(key);
    }
    localStorage.removeItem("remember_me");
}
