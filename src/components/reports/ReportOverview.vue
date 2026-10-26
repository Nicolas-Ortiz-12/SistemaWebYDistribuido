<template>
    <AlertBox v-if="report.truncated.value" type="warning">
        El período tiene demasiados registros: se muestran los primeros {{ formatNumber(report.items.value.length) }}.
        Acotá las fechas para ver el detalle completo.
    </AlertBox>

    <div class="grid grid-4">
        <StatCard :icon="IconReceipt" :label="countLabel" :value="formatNumber(report.summary.value.count)"
            :loading="report.loading.value" />
        <StatCard :icon="IconCoin" tone="info" label="Subtotal" :value="formatMoney(report.summary.value.subtotal)"
            :hint="`IVA ${formatMoney(report.summary.value.iva)}`" :loading="report.loading.value" />
        <StatCard :icon="IconCash" tone="success" label="Total" :value="formatMoney(report.summary.value.total)"
            :loading="report.loading.value" />
        <StatCard :icon="IconCalculator" tone="warning" label="Promedio por documento"
            :value="formatMoney(report.summary.value.promedio)" :loading="report.loading.value" />
    </div>

    <div class="grid grid-3">
        <section class="card span-2">
            <header class="card-header">
                <h2 class="card-title"><IconChartBar :size="18" /> Total por día</h2>
            </header>
            <div class="card-body">
                <BarChart :data="chart" :format="formatMoney" aria-label="Total por día del período" />
            </div>
        </section>

        <section class="card">
            <header class="card-header">
                <h2 class="card-title"><IconCalendarStats :size="18" /> Resumen diario</h2>
            </header>
            <EmptyState v-if="!activeDays.length" title="Sin movimientos en el período" />
            <div v-else class="table-wrap daily">
                <table class="table">
                    <thead>
                        <tr><th>Día</th><th class="num">Cant.</th><th class="num">Total</th></tr>
                    </thead>
                    <tbody>
                        <tr v-for="d in activeDays" :key="d.key">
                            <td class="nowrap">{{ formatDate(d.key) }}</td>
                            <td class="num">{{ d.count }}</td>
                            <td class="num">{{ formatMoney(d.total) }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import { IconCalculator, IconCalendarStats, IconCash, IconChartBar, IconCoin, IconReceipt } from '@tabler/icons-vue'
import AlertBox from '../ui/AlertBox.vue'
import StatCard from '../ui/StatCard.vue'
import BarChart from '../ui/BarChart.vue'
import EmptyState from '../ui/EmptyState.vue'
import { formatDate, formatMoney, formatNumber, formatShortDay } from '../../utils/format'

const props = defineProps({
    report: { type: Object, required: true },
    countLabel: { type: String, required: true },
})

const chart = computed(() =>
    [...props.report.daily.value].reverse().map(d => ({ key: d.key, label: formatShortDay(d.key), value: d.total })))

const activeDays = computed(() => props.report.daily.value.filter(d => d.count > 0))
</script>

<style scoped>
.daily { max-height: 260px; overflow-y: auto; }
</style>
