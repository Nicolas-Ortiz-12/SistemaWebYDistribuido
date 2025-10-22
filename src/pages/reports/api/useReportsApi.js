// Sencillo envoltorio fetch con manejo de errores
export async function apiGet(path, params = {}) {
    const url = new URL(`/api${path}`, window.location.origin)
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v)
    })
    const res = await fetch(url.toString(), { headers: { 'Accept': 'application/json' } })
    if (!res.ok) {
        const text = await res.text().catch(() => '')
        throw new Error(`HTTP ${res.status} - ${text || res.statusText}`)
    }
    return res.json()
}

// Endpoints según tu OpenAPI (ajusta si cambiaste)
export const ReportsApi = {
    ventasDiarias: (desde, hasta) =>
        apiGet('/reportes/ventas-diarias', { desde, hasta }),

}
