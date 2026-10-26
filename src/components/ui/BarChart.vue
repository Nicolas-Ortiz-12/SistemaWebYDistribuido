<template>
    <div class="chart" role="img" :aria-label="ariaLabel">
        <div class="chart-plot">
            <div v-for="item in bars" :key="item.key" class="chart-col" :title="`${item.label}: ${format(item.value)}`">
                <div class="chart-bar" :class="{ empty: item.value === 0 }" :style="{ height: `${item.pct}%` }" />
            </div>
        </div>
        <div class="chart-axis">
            <span v-for="(item, i) in bars" :key="item.key" class="chart-label">
                {{ showLabel(i) ? item.label : '' }}
            </span>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    // [{ key, label, value }]
    data: { type: Array, required: true },
    format: { type: Function, default: v => String(v) },
    ariaLabel: { type: String, default: 'Gráfico de barras' },
})

const bars = computed(() => {
    const max = Math.max(0, ...props.data.map(d => d.value))
    return props.data.map(d => ({ ...d, pct: max > 0 ? Math.max(2, (d.value / max) * 100) : 2 }))
})

// Con muchas barras se muestra una etiqueta cada tanto para que no se encimen
function showLabel(i) {
    const step = Math.ceil(bars.value.length / 10)
    return i % step === 0 || i === bars.value.length - 1
}
</script>

<style scoped>
.chart { display: flex; flex-direction: column; gap: 6px; }
.chart-plot { display: flex; align-items: flex-end; gap: 4px; height: 150px; border-bottom: 1px solid var(--border); }
.chart-col { flex: 1; height: 100%; display: flex; align-items: flex-end; min-width: 0; }
.chart-bar { width: 100%; border-radius: 2px 2px 0 0; background: var(--ink); transition: height .3s ease; }
.chart-bar.empty { background: var(--border); }
.chart-col:hover .chart-bar:not(.empty) { background: var(--action); }
.chart-axis { display: flex; gap: 4px; }
.chart-label { flex: 1; min-width: 0; font-size: 11px; color: var(--muted); text-align: center; white-space: nowrap; overflow: visible; }
</style>
