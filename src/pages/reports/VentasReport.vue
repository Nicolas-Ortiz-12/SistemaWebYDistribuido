<template>
    <ReportFilters :report="report" party-label="Cliente" party-placeholder="Todos los clientes" :search="searchClientes"
        @export="exportCsv" />
    <AlertBox v-if="report.error.value" type="error">{{ report.error.value }}</AlertBox>
    <ReportOverview :report="report" count-label="Ventas" />

    <section class="card">
        <header class="card-header">
            <h2 class="card-title"><IconReceipt :size="18" /> Detalle de ventas</h2>
        </header>
        <div v-if="report.loading.value" class="loading-row"><IconLoader2 :size="20" class="spin" /> Cargando ventas…</div>
        <EmptyState v-else-if="!report.items.value.length" title="No hay ventas en el período elegido" />
        <div v-else class="table-wrap">
            <table class="table">
                <thead>
                    <tr>
                        <th class="col-actions"><span class="sr-only">Ver ítems</span></th>
                        <th>Factura</th>
                        <th>Fecha</th>
                        <th>Cliente</th>
                        <th class="num">Ítems</th>
                        <th class="num">Subtotal</th>
                        <th class="num">IVA</th>
                        <th class="num">Total</th>
                    </tr>
                </thead>
                <tbody>
                    <template v-for="v in paged" :key="v.id">
                        <tr :class="{ expanded: expanded.has(v.id) }">
                            <td class="col-actions">
                                <button type="button" class="icon-btn" :aria-expanded="expanded.has(v.id)"
                                    :aria-label="`Ver ítems de la factura ${formatInvoiceNumber(v.id)}`" @click="toggle(v.id)">
                                    <IconChevronDown v-if="expanded.has(v.id)" :size="18" />
                                    <IconChevronRight v-else :size="18" />
                                </button>
                            </td>
                            <td class="cell-main mono">{{ formatInvoiceNumber(v.id) }}</td>
                            <td class="nowrap">{{ formatDateTime(v.fechaVenta) }}</td>
                            <td>{{ clienteNombre(v.clienteId) }}</td>
                            <td class="num">{{ v.items?.length ?? 0 }}</td>
                            <td class="num">{{ formatMoney(v.subtotal) }}</td>
                            <td class="num">{{ formatMoney(v.iva) }}</td>
                            <td class="num cell-main">{{ formatMoney(v.total) }}</td>
                        </tr>
                        <tr v-if="expanded.has(v.id)" class="detail-row">
                            <td colspan="8">
                                <table class="table-inner">
                                    <thead>
                                        <tr><th>Producto</th><th class="num">Cantidad</th><th class="num">Precio unit.</th>
                                            <th class="num">IVA</th><th class="num">Total</th></tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="it in v.items ?? []" :key="it.productoId">
                                            <td>{{ productoNombre(it.productoId) }}</td>
                                            <td class="num">{{ formatNumber(it.cantidad) }}</td>
                                            <td class="num">{{ formatMoney(unitPrice(it)) }}</td>
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
import { IconChevronDown, IconChevronRight, IconLoader2, IconReceipt } from '@tabler/icons-vue'
import AlertBox from '../../components/ui/AlertBox.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import ListPager from '../../components/ui/ListPager.vue'
import ReportFilters from '../../components/reports/ReportFilters.vue'
import ReportOverview from '../../components/reports/ReportOverview.vue'
import { fetchAllPages, ventasApi } from '../../api/resources'
import { catalogs } from '../../api/catalogs'
import { searchClientes } from '../../api/searchers'
import { useReport } from '../../composables/useReport'
import { downloadCsv } from '../../utils/csv'
import { endOfDayIso, localDayKey, startOfDayIso } from '../../utils/dates'
import { formatDateTime, formatInvoiceNumber, formatMoney, formatNumber } from '../../utils/format'

const report = useReport({
    load: ({ desde, hasta, partyId }) => fetchAllPages(p => ventasApi.list({
        ...p,
        clienteId: partyId ?? undefined,
        // Inicio y fin del día en hora local, con offset: el backend lo pasa a UTC
        desde: startOfDayIso(desde),
        hasta: endOfDayIso(hasta),
    })),
    dayOf: v => localDayKey(v.fechaVenta),
})

const productos = ref(new Map())
const clientes = ref(new Map())
const productoNombre = id => productos.value.get(id)?.nombre ?? `Producto #${id}`
const clienteNombre = id => (id == null ? 'Consumidor final' : clientes.value.get(id)?.nombre ?? `Cliente #${id}`)
const unitPrice = it => (Number(it.cantidad) ? (Number(it.subtotalLinea) || 0) / Number(it.cantidad) : 0)

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
        `ventas_${report.filters.desde}_${report.filters.hasta}.csv`,
        ['Factura', 'Fecha', 'Cliente', 'Ítems', 'Subtotal', 'IVA', 'Total'],
        report.items.value.map(v => [
            formatInvoiceNumber(v.id), formatDateTime(v.fechaVenta), clienteNombre(v.clienteId), v.items?.length ?? 0,
            Number(v.subtotal) || 0, Number(v.iva) || 0, Number(v.total) || 0,
        ]),
    )
}

onMounted(async () => {
    report.run()
    const [p, c] = await Promise.allSettled([catalogs.productos.load(), catalogs.clientes.load()])
    if (p.status === 'fulfilled') productos.value = p.value.byId
    if (c.status === 'fulfilled') clientes.value = c.value.byId
})
</script>
