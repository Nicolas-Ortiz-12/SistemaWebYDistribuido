import { reactive, readonly } from "vue";
import { refreshToken } from "../services/authService.js";

// estado global mínimo (puedes sustituir por Pinia/Vuex después)
const state = reactive({
    user: localStorage.getItem("username") ? { username: localStorage.getItem("username") } : null,
    isRefreshing: false
});

export function useAuth() {
    function setUser(u) { state.user = u ? { ...u } : null; }
    function logout() {
        localStorage.removeItem("access_token");
        localStorage.removeItem("refresh_token");
        localStorage.removeItem("expires_in_minutes");
        localStorage.removeItem("username");
        state.user = null;
    }

    async function ensureTokenFresh() {
        // simple: no clock skew check, intenta refresh si falta token
        const token = localStorage.getItem("access_token");
        if (token) return token;
        const rt = localStorage.getItem("refresh_token");
        if (!rt) throw new Error("no_refresh_token");
        if (state.isRefreshing) {
            // espera pasiva hasta que termine (poll breve)
            while (state.isRefreshing) await new Promise(r => setTimeout(r, 200));
            const t2 = localStorage.getItem("access_token");
            if (!t2) throw new Error("refresh_failed");
            return t2;
        }
        try {
            state.isRefreshing = true;
            const r = await refreshToken(rt);
            localStorage.setItem("access_token", r.access_token);
            localStorage.setItem("token_type", r.token_type || "Bearer");
            localStorage.setItem("expires_in_minutes", String(r.expires_in_minutes || 60));
            return r.access_token;
        } finally {
            state.isRefreshing = false;
        }
    }

    return {
        user: readonly(state.user),
        setUser,
        logout,
        ensureTokenFresh
    };
}
