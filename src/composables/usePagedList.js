// Estado de un listado paginado del backend con búsqueda: debounce del texto, cancelación
// del request anterior y vuelta atrás si se borró el último registro de la última página.
import { onMounted, onUnmounted, ref, watch } from 'vue'

const SEARCH_DEBOUNCE_MS = 300

export function usePagedList(fetchPage, { pageSize: initialSize = 10 } = {}) {
    const q = ref('')
    const page = ref(0)
    const pageSize = ref(initialSize)
    const rows = ref([])
    const total = ref(0)
    const totalPages = ref(0)
    const loading = ref(false)
    const error = ref('')

    let controller = null
    let timer = null

    async function load() {
        controller?.abort()
        const current = (controller = new AbortController())
        loading.value = true
        error.value = ''
        try {
            const data = await fetchPage({ page: page.value, size: pageSize.value, q: q.value, signal: current.signal })
            rows.value = data?.content ?? []
            total.value = Number(data?.totalElements ?? rows.value.length)
            totalPages.value = Number(data?.totalPages ?? 1)
            if (rows.value.length === 0 && page.value > 0 && page.value >= totalPages.value) {
                page.value = Math.max(0, totalPages.value - 1)
            }
        } catch (e) {
            if (e.name !== 'AbortError') error.value = e.message
        } finally {
            if (controller === current) loading.value = false
        }
    }

    function resetToFirstPage() {
        if (page.value !== 0) page.value = 0
        else load()
    }

    watch(q, () => {
        clearTimeout(timer)
        timer = setTimeout(resetToFirstPage, SEARCH_DEBOUNCE_MS)
    })
    watch(pageSize, resetToFirstPage)
    watch(page, load)

    onMounted(load)
    onUnmounted(() => {
        controller?.abort()
        clearTimeout(timer)
    })

    return { q, page, pageSize, rows, total, totalPages, loading, error, reload: load }
}
