// Catálogos completos en memoria (productos, clientes, proveedores, categorías) para mostrar
// nombres en reportes y en el dashboard sin pedir cada registro por separado.
import { categoriasApi, clientesApi, fetchAllPages, productosApi, proveedoresApi } from './resources'

const TTL_MS = 2 * 60_000

function createCatalog(api) {
    let cached = null
    let loadedAt = 0

    return {
        // Devuelve { items, byId }; se reutiliza durante TTL_MS
        load() {
            if (!cached || Date.now() - loadedAt > TTL_MS) {
                loadedAt = Date.now()
                cached = fetchAllPages(params => api.list(params)).then(({ items }) => ({
                    items,
                    byId: new Map(items.map(item => [item.id, item])),
                }))
                cached.catch(() => { cached = null })
            }
            return cached
        },
        invalidate() {
            cached = null
        },
    }
}

export const catalogs = {
    productos: createCatalog(productosApi),
    clientes: createCatalog(clientesApi),
    proveedores: createCatalog(proveedoresApi),
    categorias: createCatalog(categoriasApi),
}
