// src/pages/reports/api/useSalesApi.js
import { fetchWithAuth } from "../../../services/authService"
import { API_BASE } from "../../../services/api"

function ventasPath({ page = 0, size = 20, clienteId, desde, hasta }) {
    let path = `/ventas/${page}/${size}`
    if (clienteId !== undefined && clienteId !== "" && clienteId !== null) {
        path += `/${clienteId}`
        if (desde) {
            path += `/${encodeURIComponent(desde)}`
            if (hasta) path += `/${encodeURIComponent(hasta)}`
        }
    }
    return path
}

async function apiGet(path) {
    // fetchWithAuth ya maneja Authorization + refresh + parseo de error JSON
    return fetchWithAuth(`${API_BASE}${path}`, {
        headers: { Accept: "application/json" },
    })
}

export async function getVentasPage({
    page = 0,
    size = 50,
    clienteId,
    desde,
    hasta,
}) {
    const data = await apiGet(
        ventasPath({ page, size, clienteId, desde, hasta })
    )
    return {
        content: Array.isArray(data?.content) ? data.content : [],
        totalPages: data?.totalPages ?? 1,
        totalElements:
            data?.totalElements ?? (data?.content?.length ?? 0),
        number: data?.number ?? page,
        size: data?.size ?? size,
    }
}

// 🔹 Recorre páginas y junta todo (front-only)
export async function getVentasAll({
    clienteId,
    desde,
    hasta,
    size = 50,
    MAX_PAGES = 40,
}) {
    let page = 0
    const first = await getVentasPage({ page, size, clienteId, desde, hasta })
    const total = Math.min(Number(first.totalPages || 1), MAX_PAGES)

    let all = [...first.content]
    while (++page < total) {
        const p = await getVentasPage({ page, size, clienteId, desde, hasta })
        all.push(...p.content)
    }
    return all
}

// Producto por id (para traer el nombre / precio)
export async function getProductoById(id) {
    return apiGet(`/productos/${id}`)
}
