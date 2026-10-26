// Estado de la sesión: tokens JWT emitidos por el servicio de Seguridad.
// Se guardan en sessionStorage para que la sesión termine al cerrar la pestaña: las PCs de
// caja suelen ser compartidas y un refresh token persistente dejaría la sesión abierta días.
import { reactive, computed } from 'vue'
import { API_BASE } from '../config'

const STORAGE_KEY = 'sw.session'
// Se renueva el access token un poco antes de que venza para no mandar requests con un token muerto
const EXPIRY_MARGIN_MS = 30_000

export class AuthError extends Error {
    constructor(message) {
        super(message)
        this.name = 'AuthError'
    }
}

function decodePayload(token) {
    if (typeof token !== 'string') return null
    try {
        const part = token.split('.')[1]
        const b64 = part.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(part.length / 4) * 4, '=')
        const bytes = Uint8Array.from(atob(b64), c => c.charCodeAt(0))
        const payload = JSON.parse(new TextDecoder().decode(bytes))
        return payload && typeof payload === 'object' ? payload : null
    } catch {
        return null
    }
}

// Solo se mira el vencimiento para decidir cuándo renovar: la firma la valida el backend en cada request
function isUsable(token, marginMs = 0) {
    const p = decodePayload(token)
    return !!p && typeof p.exp === 'number' && p.exp * 1000 - marginMs > Date.now()
}

function readStorage() {
    try {
        const saved = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null')
        if (saved && (isUsable(saved.accessToken) || isUsable(saved.refreshToken))) {
            return { accessToken: saved.accessToken ?? null, refreshToken: saved.refreshToken ?? null }
        }
    } catch { /* storage bloqueado o corrupto: se arranca sin sesión */ }
    return { accessToken: null, refreshToken: null }
}

// El front anterior guardaba los tokens en localStorage bajo "auth": se borran para que no
// queden indefinidamente en las PCs que lo usaron
try {
    localStorage.removeItem('auth')
} catch { /* storage bloqueado */ }

const state = reactive(readStorage())

function persist() {
    try {
        if (state.accessToken || state.refreshToken) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state))
        else sessionStorage.removeItem(STORAGE_KEY)
    } catch { /* sin storage la sesión dura lo que la página */ }
}

function clear() {
    state.accessToken = null
    state.refreshToken = null
    persist()
}

export const currentUser = computed(() => {
    const p = decodePayload(state.accessToken) ?? decodePayload(state.refreshToken)
    if (!p?.sub) return null
    return { username: String(p.sub), roles: Array.isArray(p.roles) ? p.roles.map(String) : [] }
})

// Los roles del token solo deciden qué se muestra; los permisos reales los aplica el backend
export function hasRole(...roles) {
    return currentUser.value?.roles.some(r => roles.includes(r)) ?? false
}

export function isAuthenticated() {
    return isUsable(state.accessToken) || isUsable(state.refreshToken)
}

export async function login(username, password) {
    let resp
    try {
        resp = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify({ username, password }),
            credentials: 'omit',
        })
    } catch {
        throw new AuthError('No se pudo conectar con el servidor. Verificá que el sistema esté en línea.')
    }

    if (resp.status === 401 || resp.status === 403) throw new AuthError('Usuario o contraseña incorrectos.')
    if (resp.status === 429) {
        const body = await resp.json().catch(() => null)
        throw new AuthError(typeof body?.message === 'string' ? body.message : 'Demasiados intentos fallidos. Esperá unos minutos.')
    }
    if (resp.status === 400) throw new AuthError('Completá usuario y contraseña.')
    if (!resp.ok) throw new AuthError('No se pudo iniciar sesión. Intentá de nuevo más tarde.')

    const data = await resp.json().catch(() => null)
    if (!isUsable(data?.access_token)) throw new AuthError('El servidor devolvió una respuesta inválida.')
    state.accessToken = data.access_token
    state.refreshToken = isUsable(data.refresh_token) ? data.refresh_token : null
    persist()
}

let refreshing = null

// Pide un access token nuevo con el refresh token. Las llamadas simultáneas comparten la misma
// renovación para no disparar varios POST /auth/refresh a la vez.
export function refreshAccessToken() {
    if (!isUsable(state.refreshToken)) return Promise.resolve(false)
    refreshing ??= (async () => {
        try {
            const resp = await fetch(`${API_BASE}/auth/refresh`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify({ refresh_token: state.refreshToken }),
                credentials: 'omit',
            })
            if (!resp.ok) return false
            const data = await resp.json().catch(() => null)
            if (!isUsable(data?.access_token)) return false
            state.accessToken = data.access_token
            persist()
            return true
        } catch {
            return false
        } finally {
            refreshing = null
        }
    })()
    return refreshing
}

export async function getValidAccessToken() {
    if (isUsable(state.accessToken, EXPIRY_MARGIN_MS)) return state.accessToken
    return (await refreshAccessToken()) ? state.accessToken : null
}

let expiredHandler = () => {}

export function onSessionExpired(handler) {
    expiredHandler = handler
}

export function expireSession() {
    clear()
    expiredHandler()
}

export function logout() {
    clear()
}
