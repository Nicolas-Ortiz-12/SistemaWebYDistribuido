// Estado de un reporte por rango de fechas: filtros, carga, totales y resumen por día.
import { computed, reactive, ref } from 'vue'
import { addDays, dayRange, daysBetween, parseDateInput, toDateInput } from '../utils/dates'

export const PRESETS = [
    { key: 'hoy', label: 'Hoy' },
    { key: '7d', label: 'Últimos 7 días' },
    { key: '30d', label: 'Últimos 30 días' },
    { key: 'mes', label: 'Este mes' },
    { key: 'custom', label: 'Personalizado' },
]
const MAX_RANGE_DAYS = 366

function presetRange(key) {
    const now = new Date()
    switch (key) {
        case 'hoy': return [toDateInput(now), toDateInput(now)]
        case '7d': return [toDateInput(addDays(now, -6)), toDateInput(now)]
        case 'mes': return [toDateInput(new Date(now.getFullYear(), now.getMonth(), 1)), toDateInput(now)]
        default: return [toDateInput(addDays(now, -29)), toDateInput(now)]
    }
}

// load(filtros) -> Promise<{ items, truncated }>; dayOf(item) -> "yyyy-MM-dd" del documento
export function useReport({ load, dayOf }) {
    const [desde, hasta] = presetRange('30d')
    const filters = reactive({ preset: '30d', desde, hasta, partyId: null, partyLabel: '' })
    const items = ref([])
    const truncated = ref(false)
    const loading = ref(false)
    const error = ref('')
    const filterError = ref('')

    function applyPreset(key) {
        filters.preset = key
        if (key !== 'custom') [filters.desde, filters.hasta] = presetRange(key)
    }

    function validateFilters() {
        if (!parseDateInput(filters.desde) || !parseDateInput(filters.hasta)) return 'Ingresá las dos fechas del período.'
        const days = daysBetween(filters.desde, filters.hasta)
        if (days < 0) return 'La fecha “desde” no puede ser posterior a “hasta”.'
        if (days > MAX_RANGE_DAYS) return 'El período no puede superar un año.'
        return ''
    }

    async function run() {
        filterError.value = validateFilters()
        if (filterError.value) return
        loading.value = true
        error.value = ''
        try {
            const result = await load({ desde: filters.desde, hasta: filters.hasta, partyId: filters.partyId })
            items.value = [...result.items].sort((a, b) => (dayOf(b) ?? '').localeCompare(dayOf(a) ?? '') || b.id - a.id)
            truncated.value = result.truncated
        } catch (e) {
            error.value = e.message
            items.value = []
        } finally {
            loading.value = false
        }
    }

    const summary = computed(() => {
        const sum = field => items.value.reduce((s, x) => s + (Number(x[field]) || 0), 0)
        const count = items.value.length
        const total = sum('total')
        return { count, subtotal: sum('subtotal'), iva: sum('iva'), total, promedio: count ? total / count : 0 }
    })

    // Un renglón por día del período (con o sin movimientos), del más reciente al más antiguo
    const daily = computed(() => {
        const byDay = new Map()
        for (const item of items.value) {
            const key = dayOf(item)
            const row = byDay.get(key) ?? { key, count: 0, subtotal: 0, iva: 0, total: 0 }
            row.count++
            row.subtotal += Number(item.subtotal) || 0
            row.iva += Number(item.iva) || 0
            row.total += Number(item.total) || 0
            byDay.set(key, row)
        }
        return dayRange(filters.desde, filters.hasta)
            .map(key => byDay.get(key) ?? { key, count: 0, subtotal: 0, iva: 0, total: 0 })
            .reverse()
    })

    return { filters, items, truncated, loading, error, filterError, summary, daily, applyPreset, run }
}
