// Endpoints del backend agrupados por recurso. Los listados devuelven un Page<> de Spring
// ({ content, totalElements, totalPages, number, size }).
import { buildPath, http } from './http'

// Tope de "size" que acepta el backend (Paginas.MAX_SIZE)
export const MAX_PAGE_SIZE = 200

function crud(resource) {
    return {
        // La búsqueda va por ?q=: en la ruta, un texto como "1/2 pulgada" sería rechazado
        list: ({ page = 0, size = 10, q, signal } = {}) =>
            http.get(buildPath(resource, page, size), { query: { q: q?.trim() || undefined }, signal }),
        get: (id, options) => http.get(buildPath(resource, id), options),
        create: body => http.post(buildPath(resource), body),
        update: (id, body) => http.put(buildPath(resource, id), body),
        remove: id => http.delete(buildPath(resource, id)),
    }
}

export const clientesApi = crud('clientes')
export const proveedoresApi = crud('proveedores')
export const categoriasApi = crud('categorias')
export const productosApi = crud('productos')

export const ventasApi = {
    // Número que va a tener la próxima venta: { numero: "001-001-0000024" }
    nextNumber: () => http.get(buildPath('ventas', 'proximo-numero')),
    list: ({ page = 0, size = 50, clienteId, desde, hasta, signal } = {}) =>
        http.get(buildPath('ventas', page, size), { query: { clienteId, desde, hasta }, signal }),
    create: body => http.post(buildPath('ventas'), body),
}

export const comprasApi = {
    list: ({ page = 0, size = 50, proveedorId, desde, hasta, signal } = {}) =>
        http.get(buildPath('compras', page, size), { query: { proveedorId, desde, hasta }, signal }),
    create: body => http.post(buildPath('compras'), body),
}

// Recorre las páginas de un listado hasta maxPages; "truncated" avisa si quedaron registros afuera
export async function fetchAllPages(fetchPage, { size = MAX_PAGE_SIZE, maxPages = 25 } = {}) {
    const first = await fetchPage({ page: 0, size })
    const totalPages = Number(first?.totalPages ?? 1)
    const items = [...(first?.content ?? [])]
    for (let page = 1; page < Math.min(totalPages, maxPages); page++) {
        const next = await fetchPage({ page, size })
        items.push(...(next?.content ?? []))
    }
    return { items, total: Number(first?.totalElements ?? items.length), truncated: totalPages > maxPages }
}
