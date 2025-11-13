import { fetchWithAuth } from "../../../services/authService"

const API_BASE = (
    import.meta.env.VITE_API_BASE ??
    import.meta.env.VITE_API_URL ??
    "http://localhost:8080"
).replace(/\/+$/, "")

// Rutas disponibles en tu compra-controller
function comprasPath({ page = 0, size = 50, proveedorId, desde, hasta }) {
    let path = `/compras/${page}/${size}`
    if (proveedorId !== undefined && proveedorId !== "" && proveedorId !== null) {
        path += `/${proveedorId}`
        if (desde) {
            path += `/${encodeURIComponent(desde)}`
            if (hasta) path += `/${encodeURIComponent(hasta)}`
        }
    }
    return path
}

async function apiGet(path) {
    // fetchWithAuth ya maneja Authorization + manejo de errores y parseo JSON
    return fetchWithAuth(`${API_BASE}${path}`, {
        headers: { Accept: "application/json" },
    })
}

// Página simple
export async function getComprasPage({ page = 0, size = 50, proveedorId, desde, hasta }) {
    const data = await apiGet(
        comprasPath({ page, size, proveedorId, desde, hasta })
    )
    return {
        content: Array.isArray(data?.content) ? data.content : [],
        totalPages: data?.totalPages ?? 1,
        totalElements: data?.totalElements ?? (data?.content?.length ?? 0),
        number: data?.number ?? page,
        size: data?.size ?? size,
    }
}

// Recorre páginas y junta todo (sin tocar backend)
export async function getComprasAll({
    proveedorId,
    desde,
    hasta,
    size = 50,
    MAX_PAGES = 40,
}) {
    let page = 0
    const first = await getComprasPage({ page, size, proveedorId, desde, hasta })
    const total = Math.min(Number(first.totalPages || 1), MAX_PAGES)

    let all = [...first.content]
    while (++page < total) {
        const p = await getComprasPage({ page, size, proveedorId, desde, hasta })
        all.push(...p.content)
    }
    return all
}

// Producto por id (para traer el nombre)
export async function getProductoById(id) {
    return apiGet(`/productos/${id}`)
}
