// src/pages/reports/api/ReportsApi.js
import { fetchWithAuth } from "../../../services/authService"
import { API_BASE } from "../../../services/api"

/**
 * GET seguro con params, usando fetchWithAuth
 */
export async function apiGet(path, params = {}) {
    const url = API_BASE.startsWith("http")
        ? new URL(`${API_BASE}${path}`)
        : new URL(`${API_BASE}${path}`, window.location.origin)

    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") {
            url.searchParams.set(key, value)
        }
    })

    // fetchWithAuth maneja errores 401, refresh automático y parseo JSON
    return fetchWithAuth(url.toString(), {
        method: "GET",
        headers: { Accept: "application/json" }
    })
}

/**
 * Endpoints de reportes
 * Ajusta si agregás más en tu backend
 */
export const ReportsApi = {
    ventasDiarias(desde, hasta) {
        return apiGet("/reportes/ventas-diarias", { desde, hasta })
    },

    // cuando agregues más reportes:
    // ventasPorProducto(desde, hasta) { return apiGet("/reportes/ventas-producto", { desde, hasta }) }
    // comprasPorProveedor(desde, hasta) { return apiGet("/reportes/compras-proveedor", { desde, hasta }) }
}
