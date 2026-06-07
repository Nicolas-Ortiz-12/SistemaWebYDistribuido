import { computed, reactive, readonly } from "vue";
import { refreshToken } from "../services/authService.js";
import {
    clearAuthTokens,
    getAuthStore,
    readAuthValue,
    updateAccessToken,
} from "../services/authStorage.js";

// estado global mínimo (puedes sustituir por Pinia/Vuex después)
const state = reactive({
    user: readAuthValue("username") ? { username: readAuthValue("username") } : null,
    isRefreshing: false
});

export function useAuth() {
    const user = computed(() => state.user);

    function setUser(u) {
        state.user = u ? { ...u } : null;
        const store = getAuthStore();
        if (u?.username) {
            store.setItem("username", u.username);
        } else {
            localStorage.removeItem("username");
            sessionStorage.removeItem("username");
        }
    }

    function logout() {
        clearAuthTokens();
        state.user = null;
    }

    async function ensureTokenFresh() {
        // simple: no clock skew check, intenta refresh si falta token
        const token = readAuthValue("access_token");
        if (token) return token;
        const rt = readAuthValue("refresh_token");
        if (!rt) throw new Error("no_refresh_token");
        if (state.isRefreshing) {
            // espera pasiva hasta que termine (poll breve)
            while (state.isRefreshing) await new Promise(r => setTimeout(r, 200));
            const t2 = readAuthValue("access_token");
            if (!t2) throw new Error("refresh_failed");
            return t2;
        }
        try {
            state.isRefreshing = true;
            const r = await refreshToken(rt);
            updateAccessToken(r);
            return r.access_token;
        } finally {
            state.isRefreshing = false;
        }
    }

    return {
        user: readonly(user),
        setUser,
        logout,
        ensureTokenFresh
    };
}
