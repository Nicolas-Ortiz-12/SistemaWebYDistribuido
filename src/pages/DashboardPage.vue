<template>
    <PageHeader :title="`Hola, ${currentUser?.username ?? ''}`" :subtitle="capitalize(formatLongDate(new Date()))"
        :icon="IconLayoutDashboard">
        <RouterLink class="btn" to="/existencias/compras"><IconFileInvoice :size="18" /> Ingresar compra</RouterLink>
        <RouterLink class="btn btn-primary" to="/existencias/ventas"><IconCashRegister :size="18" /> Nueva venta</RouterLink>
    </PageHeader>

    <AlertBox v-if="failed.length" type="warning">
        No se pudo cargar: {{ failed.join(', ') }}. El resto de la información está actualizada.
    </AlertBox>
    <AlertBox v-if="ventas.truncated" type="info">
        Hay muchas ventas en los últimos 30 días: el resumen usa las primeras {{ formatNumber(ventas.items.length) }}.
    </AlertBox>

    <div class="grid grid-3">
        <StatCard :icon="IconCash" label="Ventas de hoy" :value="formatMoney(hoy.total)" :hint="plural(hoy.count, 'venta')"
            :loading="loading" />
        <StatCard :icon="IconTrendingUp" tone="info" label="Ventas · últimos 30 días" :value="formatMoney(mes.total)"
            :hint="plural(mes.count, 'venta')" :loading="loading" />
        <StatCard :icon="IconTruckDelivery" tone="warning" label="Compras · últimos 30 días"
            :value="formatMoney(compras.total)" :hint="plural(compras.count, 'compra')" :loading="loading" />
    </div>

    <div class="grid grid-3">
        <StatCard :icon="IconPackage" label="Productos" :value="formatNumber(counts.productos)" :loading="loading" />
        <StatCard :icon="IconUsers" tone="info" label="Clientes" :value="formatNumber(counts.clientes)" :loading="loading" />
        <StatCard :icon="IconTruck" tone="success" label="Proveedores" :value="formatNumber(counts.proveedores)"
            :loading="loading" />
    </div>

    <div class="grid grid-3">
        <section class="card span-2">
            <header class="card-header">
                <h2 class="card-title"><IconChartBar :size="18" /> Ventas por día</h2>
                <span class="muted">Últimos 14 días · {{ formatMoney(chartTotal) }}</span>
            </header>
            <div class="card-body">
                <div v-if="loading" class="loading-row"><IconLoader2 :size="20" class="spin" /> Cargando…</div>
                <BarChart v-else :data="chart" :format="formatMoney" aria-label="Total vendido por día en los últimos 14 días" />
            </div>
        </section>

        <section class="card">
            <header class="card-header">
                <h2 class="card-title">Más vendidos</h2>
                <span class="muted">30 días</span>
            </header>
            <div v-if="loading" class="loading-row"><IconLoader2 :size="20" class="spin" /></div>
            <EmptyState v-else-if="!topProductos.length" title="Sin ventas en el período" />
            <ol v-else class="list">
                <li v-for="(p, i) in topProductos" :key="p.id" class="list-item">
                    <span class="rank">{{ i + 1 }}</span>
                    <div class="spacer">
                        <div class="cell-main">{{ p.nombre }}</div>
                        <div class="progress"><div class="progress-bar" :style="{ width: `${p.pct}%` }" /></div>
                    </div>
                    <span class="mono nowrap">{{ formatNumber(p.cantidad) }}</span>
                </li>
            </ol>
        </section>
    </div>

    <div class="grid grid-3">
        <section class="card span-2">
            <header class="card-header">
                <h2 class="card-title"><IconClock :size="18" /> Últimas ventas</h2>
                <RouterLink class="btn btn-sm btn-ghost" to="/reportes">Ver reporte <IconArrowRight :size="16" /></RouterLink>
            </header>
            <div v-if="loading" class="loading-row"><IconLoader2 :size="20" class="spin" /></div>
            <EmptyState v-else-if="!ultimas.length" title="Todavía no hay ventas en los últimos 30 días" />
            <div v-else class="table-wrap">
                <table class="table">
                    <thead>
                        <tr><th>Factura</th><th>Cliente</th><th>Fecha</th><th class="num">Total</th></tr>
                    </thead>
                    <tbody>
                        <tr v-for="v in ultimas" :key="v.id">
                            <td class="cell-main mono">{{ formatInvoiceNumber(v.id) }}</td>
                            <td>{{ clienteNombre(v.clienteId) }}</td>
                            <td class="nowrap">{{ formatDateTime(v.fechaVenta) }}</td>
                            <td class="num">{{ formatMoney(v.total) }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="card">
            <header class="card-header">
                <h2 class="card-title"><IconAlertTriangle :size="18" /> Stock bajo</h2>
                <span class="muted">≤ {{ LOW_STOCK }} unidades</span>
            </header>
            <div v-if="loading" class="loading-row"><IconLoader2 :size="20" class="spin" /></div>
            <EmptyState v-else-if="!stockBajo.length" :icon="IconCircleCheck" title="Todo el stock está en orden" />
            <ul v-else class="list">
                <li v-for="p in stockBajo" :key="p.id" class="list-item">
                    <div>
                        <div class="cell-main">{{ p.nombre }}</div>
                        <div class="cell-sub">{{ p.codigo || 'Sin código' }}</div>
                    </div>
                    <span class="badge" :class="stockBadge(stockOf(p))">{{ formatNumber(stockOf(p)) }} {{ p.unidadMedida }}</span>
                </li>
            </ul>
        </section>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
    IconAlertTriangle, IconArrowRight, IconCash, IconCashRegister, IconChartBar, IconCircleCheck, IconClock,
    IconFileInvoice, IconLayoutDashboard, IconLoader2, IconPackage, IconTrendingUp, IconTruck,
    IconTruckDelivery, IconUsers,
} from '@tabler/icons-vue'
import PageHeader from '../components/ui/PageHeader.vue'
import AlertBox from '../components/ui/AlertBox.vue'
import StatCard from '../components/ui/StatCard.vue'
import EmptyState from '../components/ui/EmptyState.vue'
import BarChart from '../components/ui/BarChart.vue'
import { currentUser } from '../auth/session'
import { clientesApi, comprasApi, fetchAllPages, productosApi, proveedoresApi, ventasApi } from '../api/resources'
import { catalogs } from '../api/catalogs'
import { addDays, dayRange, endOfDayIso, localDayKey, startOfDayIso, toDateInput } from '../utils/dates'
import { formatDateTime, formatInvoiceNumber, formatLongDate, formatMoney, formatNumber, formatShortDay } from '../utils/format'
import { LOW_STOCK, stockBadge, stockOf } from '../utils/stock'

const today = toDateInput()
const desde30 = toDateInput(addDays(new Date(), -29))

const loading = ref(true)
const failed = ref([])
const ventas = reactive({ items: [], truncated: false })
const comprasItems = ref([])
const counts = reactive({ productos: 0, clientes: 0, proveedores: 0 })
const productos = ref(new Map())
const clientes = ref(new Map())

const capitalize = s => s.charAt(0).toUpperCase() + s.slice(1)
const plural = (n, word) => `${formatNumber(n)} ${word}${n === 1 ? '' : 's'}`
const sumTotal = list => list.reduce((s, x) => s + (Number(x.total) || 0), 0)

const hoy = computed(() => {
    const list = ventas.items.filter(v => localDayKey(v.fechaVenta) === today)
    return { count: list.length, total: sumTotal(list) }
})

const mes = computed(() => ({ count: ventas.items.length, total: sumTotal(ventas.items) }))

const compras = computed(() => ({ count: comprasItems.value.length, total: sumTotal(comprasItems.value) }))

const chart = computed(() => {
    const byDay = new Map()
    for (const v of ventas.items) {
        const key = localDayKey(v.fechaVenta)
        byDay.set(key, (byDay.get(key) ?? 0) + (Number(v.total) || 0))
    }
    return dayRange(toDateInput(addDays(new Date(), -13)), today)
        .map(key => ({ key, label: formatShortDay(key), value: byDay.get(key) ?? 0 }))
})
const chartTotal = computed(() => chart.value.reduce((s, d) => s + d.value, 0))

const topProductos = computed(() => {
    const qty = new Map()
    for (const v of ventas.items) {
        for (const it of v.items ?? []) qty.set(it.productoId, (qty.get(it.productoId) ?? 0) + (Number(it.cantidad) || 0))
    }
    const top = [...qty.entries()].sort((a, b) => b[1] - a[1]).slice(0, 5)
    const max = top[0]?.[1] || 1
    return top.map(([id, cantidad]) => ({
        id,
        cantidad,
        nombre: productos.value.get(id)?.nombre ?? `Producto #${id}`,
        pct: (cantidad / max) * 100,
    }))
})

const stockBajo = computed(() =>
    [...productos.value.values()]
        .filter(p => stockOf(p) <= LOW_STOCK)
        .sort((a, b) => stockOf(a) - stockOf(b))
        .slice(0, 6))

const ultimas = computed(() =>
    [...ventas.items].sort((a, b) => new Date(b.fechaVenta) - new Date(a.fechaVenta)).slice(0, 6))

const clienteNombre = id => (id == null ? 'Consumidor final' : clientes.value.get(id)?.nombre ?? `Cliente #${id}`)

const totalOf = api => api.list({ page: 0, size: 1 }).then(p => Number(p?.totalElements ?? 0))

onMounted(async () => {
    const tasks = {
        ventas: fetchAllPages(p => ventasApi.list({ ...p, desde: startOfDayIso(desde30), hasta: endOfDayIso(today) }), { maxPages: 10 })
            .then(r => { ventas.items = r.items; ventas.truncated = r.truncated }),
        compras: fetchAllPages(p => comprasApi.list({ ...p, desde: desde30, hasta: today }), { maxPages: 10 })
            .then(r => { comprasItems.value = r.items }),
        productos: Promise.all([totalOf(productosApi), catalogs.productos.load()])
            .then(([n, c]) => { counts.productos = n; productos.value = c.byId }),
        clientes: Promise.all([totalOf(clientesApi), catalogs.clientes.load()])
            .then(([n, c]) => { counts.clientes = n; clientes.value = c.byId }),
        proveedores: totalOf(proveedoresApi).then(n => { counts.proveedores = n }),
    }
    const results = await Promise.allSettled(Object.values(tasks))
    failed.value = Object.keys(tasks).filter((_, i) => results[i].status === 'rejected')
    loading.value = false
})
</script>
