// Cliente HTTP para el API Gateway: agrega el JWT, renueva el token vencido una vez y
// convierte las respuestas de error en ApiError con un mensaje apto para mostrar al usuario.
import { API_BASE } from '../config'
import { expireSession, getValidAccessToken, refreshAccessToken } from '../auth/session'

export class ApiError extends Error {
    constructor(status, message) {
        super(message)
        this.name = 'ApiError'
        this.status = status
    }
}

const MESSAGES = {
    400: 'Los datos enviados no son válidos.',
    403: 'No tenés permiso para realizar esta acción.',
    404: 'El registro no existe o fue eliminado.',
    409: 'La operación entra en conflicto con datos existentes.',
    429: 'Demasiadas solicitudes. Esperá un momento e intentá de nuevo.',
}
const MAX_SERVER_MESSAGE = 300

// Arma "/a/b/c" codificando cada segmento: un texto con "/" o "?" no puede cambiar la ruta
export function buildPath(...segments) {
    return '/' + segments.map(s => encodeURIComponent(String(s))).join('/')
}

function buildUrl(path, query) {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(query ?? {})) {
        if (value !== undefined && value !== null && value !== '') params.set(key, String(value))
    }
    const qs = params.toString()
    return `${API_BASE}${path}${qs ? `?${qs}` : ''}`
}

async function errorMessage(resp) {
    const data = await resp.json().catch(() => null)
    // Los 403 y 5xx no muestran el detalle del servidor: puede ser técnico o revelar internos
    const showServer = resp.status < 500 && resp.status !== 403
    const fromServer = showServer && typeof data?.message === 'string' && data.message.trim()
        ? data.message.trim().slice(0, MAX_SERVER_MESSAGE)
        : null
    if (fromServer) return fromServer
    if (MESSAGES[resp.status]) return MESSAGES[resp.status]
    return resp.status >= 500
        ? 'Error del servidor. Intentá de nuevo más tarde.'
        : `Error inesperado (HTTP ${resp.status}).`
}

function send(method, url, token, body, signal) {
    const headers = { Accept: 'application/json', Authorization: `Bearer ${token}` }
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    return fetch(url, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        signal,
        credentials: 'omit',
    })
}

async function request(method, path, { query, body, signal } = {}) {
    const url = buildUrl(path, query)
    const token = await getValidAccessToken()
    if (!token) {
        expireSession()
        throw new ApiError(401, 'Tu sesión expiró. Volvé a ingresar.')
    }

    let resp
    try {
        resp = await send(method, url, token, body, signal)
        // El token pudo vencer o revocarse entre el chequeo y el request: se renueva una sola vez
        if (resp.status === 401 && await refreshAccessToken()) {
            resp = await send(method, url, await getValidAccessToken(), body, signal)
        }
    } catch (e) {
        if (e.name === 'AbortError') throw e
        throw new ApiError(0, 'No se pudo conectar con el servidor.')
    }

    if (resp.status === 401) {
        expireSession()
        throw new ApiError(401, 'Tu sesión expiró. Volvé a ingresar.')
    }
    if (!resp.ok) throw new ApiError(resp.status, await errorMessage(resp))
    if (resp.status === 204) return null

    const text = await resp.text()
    try {
        return text ? JSON.parse(text) : null
    } catch {
        throw new ApiError(resp.status, 'El servidor devolvió una respuesta inválida.')
    }
}

export const http = {
    get: (path, options) => request('GET', path, options),
    post: (path, body, options) => request('POST', path, { ...options, body }),
    put: (path, body, options) => request('PUT', path, { ...options, body }),
    delete: (path, options) => request('DELETE', path, options),
}
