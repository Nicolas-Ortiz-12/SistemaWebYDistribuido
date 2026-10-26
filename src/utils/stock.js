// En el backend el stock disponible de un producto se guarda en "stockMinimo"
export const LOW_STOCK = 5

export const stockOf = producto => Number(producto?.stockMinimo) || 0

export function stockBadge(stock) {
    const n = Number(stock) || 0
    if (n <= 0) return 'badge-danger'
    return n <= LOW_STOCK ? 'badge-warning' : 'badge-success'
}
