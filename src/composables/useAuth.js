import { computed, reactive, readonly } from "vue";
import { fetchWithAuth } from "../services/authService.js";
import {
    clearAuthTokens,
    getAuthStore,
    readAuthValue,
} from "../services/authStorage.js";
import { API_BASE } from "../services/api.js";

// estado global mínimo (puedes sustituir por Pinia/Vuex después)
const state = reactive({
    user: readAuthValue("username") ? { username: readAuthValue("username") } : null,
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
        try {
            const token = readAuthValue("access_token");
            const refresh = readAuthValue("refresh_token");
            if (!token && !refresh) throw new Error("no_refresh_token");

            await fetchWithAuth(`${API_BASE}/auth/me`, {
                method: "GET",
                headers: { Accept: "application/json" },
            });
            return readAuthValue("access_token");
        } catch {
            throw new Error("refresh_failed");
        }
    }

    return {
        user: readonly(user),
        setUser,
        logout,
        ensureTokenFresh
    };
}
