<template>
    <ReportFilters :report="report" party-label="Proveedor" party-placeholder="Todos los proveedores"
        :search="searchProveedores" @export="exportCsv" />
    <AlertBox v-if="report.error.value" type="error">{{ report.error.value }}</AlertBox>
    <ReportOverview :report="report" count-label="Compras" />

    <section class="card">
        <header class="card-header">
            <h2 class="card-title"><IconFileInvoice :size="18" /> Detalle de compras</h2>
        </header>
        <div v-if="report.loading.value" class="loading-row"><IconLoader2 :size="20" class="spin" /> Cargando compras…</div>
        <EmptyState v-else-if="!report.items.value.length" title="No hay compras en el período elegido" />
        <div v-else class="table-wrap">
            <table class="table">
                <thead>
                    <tr>
                        <th class="col-actions"><span class="sr-only">Ver ítems</span></th>
                        <th>Comprobante</th>
                        <th>Emisión</th>
                        <th>Proveedor</th>
                        <th class="num">Ítems</th>
                        <th class="num">Subtotal</th>
                        <th class="num">IVA</th>
                        <th class="num">Total</th>
                    </tr>
                </thead>
                <tbody>
                    <template v-for="c in paged" :key="c.id">
                        <tr :class="{ expanded: expanded.has(c.id) }">
                            <td class="col-actions">
                                <button type="button" class="icon-btn" :aria-expanded="expanded.has(c.id)"
                                    :aria-label="`Ver ítems del comprobante ${c.numero || c.id}`" @click="toggle(c.id)">
                                    <IconChevronDown v-if="expanded.has(c.id)" :size="18" />
                                    <IconChevronRight v-else :size="18" />
                                </button>
                            </td>
                            <td class="cell-main mono">{{ c.numero || `#${c.id}` }}</td>
                            <td class="nowrap">{{ formatDate(c.fechaEmision) }}</td>
                            <td>{{ proveedorNombre(c.proveedorId) }}</td>
                            <td class="num">{{ c.detalles?.length ?? 0 }}</td>
                            <td class="num">{{ formatMoney(c.subtotal) }}</td>
                            <td class="num">{{ formatMoney(c.iva) }}</td>
                            <td class="num cell-main">{{ formatMoney(c.total) }}</td>
                        </tr>
                        <tr v-if="expanded.has(c.id)" class="detail-row">
                            <td colspan="8">
                                <table class="table-inner">
                                    <thead>
                                        <tr><th>Producto</th><th class="num">Cantidad</th><th class="num">Costo unit.</th>
                                            <th class="num">IVA</th><th class="num">Total</th></tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="it in c.detalles ?? []" :key="it.productoId">
                                            <td>{{ productoNombre(it.productoId) }}</td>
                                            <td class="num">{{ formatNumber(it.cantidad) }}</td>
                                            <td class="num">{{ formatMoney(it.costoUnitario) }}</td>
                                            <td class="num">{{ formatNumber(it.tasaIva) }} %</td>
                                            <td class="num">{{ formatMoney(it.totalLinea) }}</td>
                                        </tr>
                                    </tbody>
                                </table>
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>
        <ListPager v-if="report.items.value.length" v-model:page="page" v-model:page-size="pageSize"
            :total="report.items.value.length" />
    </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { IconChevronDown, IconChevronRight, IconFileInvoice, IconLoader2 } from '@tabler/icons-vue'
import AlertBox from '../../components/ui/AlertBox.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import ListPager from '../../components/ui/ListPager.vue'
import ReportFilters from '../../components/reports/ReportFilters.vue'
import ReportOverview from '../../components/reports/ReportOverview.vue'
import { comprasApi, fetchAllPages } from '../../api/resources'
import { catalogs } from '../../api/catalogs'
import { searchProveedores } from '../../api/searchers'
import { useReport } from '../../composables/useReport'
import { downloadCsv } from '../../utils/csv'
import { formatDate, formatMoney, formatNumber } from '../../utils/format'

const report = useReport({
    // fechaEmision es un LocalDate: el filtro va como "yyyy-MM-dd", sin hora ni zona
    load: ({ desde, hasta, partyId }) => fetchAllPages(p => comprasApi.list({ ...p, proveedorId: partyId ?? undefined, desde, hasta })),
    dayOf: c => c.fechaEmision,
})

const productos = ref(new Map())
const proveedores = ref(new Map())
const productoNombre = id => productos.value.get(id)?.nombre ?? `Producto #${id}`
const proveedorNombre = id => proveedores.value.get(id)?.nombre ?? `Proveedor #${id}`

const page = ref(0)
const pageSize = ref(10)
const expanded = reactive(new Set())
const paged = computed(() => report.items.value.slice(page.value * pageSize.value, (page.value + 1) * pageSize.value))

watch(report.items, () => {
    page.value = 0
    expanded.clear()
})
watch(pageSize, () => { page.value = 0 })

function toggle(id) {
    if (expanded.has(id)) expanded.delete(id)
    else expanded.add(id)
}

function exportCsv() {
    downloadCsv(
        `compras_${report.filters.desde}_${report.filters.hasta}.csv`,
        ['Comprobante', 'Emisión', 'Proveedor', 'Ítems', 'Subtotal', 'IVA', 'Total'],
        report.items.value.map(c => [
            c.numero ?? `#${c.id}`, formatDate(c.fechaEmision), proveedorNombre(c.proveedorId), c.detalles?.length ?? 0,
            Number(c.subtotal) || 0, Number(c.iva) || 0, Number(c.total) || 0,
        ]),
    )
}

onMounted(async () => {
    report.run()
    const [p, v] = await Promise.allSettled([catalogs.productos.load(), catalogs.proveedores.load()])
    if (p.status === 'fulfilled') productos.value = p.value.byId
    if (v.status === 'fulfilled') proveedores.value = v.value.byId
})
</script>
