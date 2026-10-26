// Líneas de detalle de una venta o una compra: totales calculados como en el backend
// (IVA sumado al subtotal, redondeo a 2 decimales por línea) y validación antes de enviar.
import { computed, ref } from 'vue'
import { LIMITS } from '../utils/validation'

export const IVA_RATES = [10, 5, 0]
const MAX_LINES = 100
const MAX_QTY = 1e6

let nextKey = 1
const round2 = n => Math.round((n + Number.EPSILON) * 100) / 100

// precio: precio de lista (venta) o costo unitario (compra)
function newLine(values = {}) {
    return { key: nextKey++, productoId: null, label: '', precio: 0, stock: null, unidad: '', cantidad: 1, tasaIva: 10, ...values }
}

export function lineSubtotal(line) {
    return round2((Number(line.cantidad) || 0) * (Number(line.precio) || 0))
}

export function lineIva(line) {
    return round2(lineSubtotal(line) * (Number(line.tasaIva) || 0) / 100)
}

// checkStock: la venta no puede superar el stock. positivePrice: la compra exige costo > 0
export function useDocumentLines({ checkStock = false, positivePrice = false } = {}) {
    const lines = ref([])

    const totals = computed(() => {
        const subtotal = round2(lines.value.reduce((sum, l) => sum + lineSubtotal(l), 0))
        const iva = round2(lines.value.reduce((sum, l) => sum + lineIva(l), 0))
        return { subtotal, iva, total: round2(subtotal + iva) }
    })

    // Suma un producto elegido en el buscador; si ya está en la lista, sube su cantidad en uno
    function addProduct(option, { useCost = false } = {}) {
        const producto = option.data
        const existing = lines.value.find(l => l.productoId === option.id)
        if (existing) {
            existing.cantidad = (Number(existing.cantidad) || 0) + 1
            return existing
        }
        if (lines.value.length >= MAX_LINES) return null
        const line = newLine({
            productoId: option.id,
            label: option.label,
            precio: Number(useCost ? producto?.costo : producto?.precio) || 0,
            stock: Number(producto?.stockMinimo) || 0,
            unidad: producto?.unidadMedida || '',
        })
        lines.value.push(line)
        return line
    }

    function remove(key) {
        lines.value = lines.value.filter(l => l.key !== key)
    }

    function reset() {
        lines.value = []
    }

    // Devuelve el primer error encontrado (texto) o null
    function validateLines() {
        const used = lines.value.filter(l => l.productoId != null)
        if (used.length === 0) return 'Agregá al menos un producto.'
        const ids = new Set()
        for (const [i, l] of lines.value.entries()) {
            const n = i + 1
            if (ids.has(l.productoId)) return `El producto de la línea ${n} está repetido: sumá la cantidad en una sola línea.`
            ids.add(l.productoId)
            const qty = Number(l.cantidad)
            if (!Number.isFinite(qty) || qty <= 0) return `La cantidad de la línea ${n} debe ser mayor que cero.`
            if (qty > MAX_QTY) return `La cantidad de la línea ${n} es demasiado grande.`
            const price = Number(l.precio)
            if (!Number.isFinite(price) || price < 0 || (positivePrice && price <= 0)) {
                return positivePrice
                    ? `El costo unitario de la línea ${n} debe ser mayor que cero.`
                    : `El importe de la línea ${n} no es válido.`
            }
            if (!IVA_RATES.includes(Number(l.tasaIva))) return `La tasa de IVA de la línea ${n} no es válida.`
            if (checkStock && l.stock != null && qty > l.stock) return `No hay stock suficiente de “${l.label}” (disponible: ${l.stock}).`
        }
        return null
    }

    function toDraft() {
        return lines.value.map(({ productoId, label, precio, stock, unidad, cantidad, tasaIva }) => ({ productoId, label, precio, stock, unidad, cantidad, tasaIva }))
    }

    // Los borradores vienen de localStorage: se aceptan solo valores con la forma esperada
    function fromDraft(raw) {
        if (!Array.isArray(raw)) return
        const restored = raw.slice(0, MAX_LINES).flatMap(d => {
            const id = Number(d?.productoId)
            if (!Number.isInteger(id) || id <= 0) return []
            return [newLine({
                productoId: id,
                label: String(d.label ?? '').slice(0, LIMITS.producto.nombre + LIMITS.producto.codigo + 3),
                precio: Number.isFinite(Number(d.precio)) ? Math.max(0, Number(d.precio)) : 0,
                stock: Number.isFinite(Number(d.stock)) ? Number(d.stock) : null,
                unidad: String(d.unidad ?? '').slice(0, LIMITS.producto.unidadMedida),
                cantidad: Number.isFinite(Number(d.cantidad)) && Number(d.cantidad) > 0 ? Number(d.cantidad) : 1,
                tasaIva: IVA_RATES.includes(Number(d.tasaIva)) ? Number(d.tasaIva) : 10,
            })]
        })
        lines.value = restored
    }

    return { lines, totals, addProduct, remove, reset, validateLines, toDraft, fromDraft }
}
