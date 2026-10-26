// Funciones de búsqueda para SearchSelect: traen las primeras coincidencias del backend.
import { categoriasApi, clientesApi, productosApi, proveedoresApi } from './resources'
import { formatMoney, formatNumber } from '../utils/format'

const RESULTS = 20

export const productoLabel = p => (p.codigo ? `${p.codigo} — ${p.nombre}` : p.nombre)

export async function searchProductos(q, signal) {
    const page = await productosApi.list({ page: 0, size: RESULTS, q, signal })
    return (page?.content ?? []).map(p => ({
        id: p.id,
        label: productoLabel(p),
        hint: `${formatMoney(p.precio)} · stock ${formatNumber(p.stockMinimo)}`,
        data: p,
    }))
}

export async function searchClientes(q, signal) {
    const page = await clientesApi.list({ page: 0, size: RESULTS, q, signal })
    return (page?.content ?? []).map(c => ({ id: c.id, label: c.nombre, hint: c.ruc ? `RUC ${c.ruc}` : '', data: c }))
}

export async function searchProveedores(q, signal) {
    const page = await proveedoresApi.list({ page: 0, size: RESULTS, q, signal })
    return (page?.content ?? [])
        .filter(p => p.activo !== false)
        .map(p => ({ id: p.id, label: p.nombre, hint: p.ruc ? `RUC ${p.ruc}` : '', data: p }))
}

export async function searchCategorias(q, signal) {
    const page = await categoriasApi.list({ page: 0, size: RESULTS, q, signal })
    return (page?.content ?? []).map(c => ({ id: c.id, label: c.nombre, data: c }))
}
