<template>
    <PageHeader title="Reportes" subtitle="Ventas y compras por período, con detalle y exportación a CSV"
        :icon="IconReportAnalytics" />

    <nav class="card tabs" aria-label="Tipo de reporte">
        <button v-for="t in TABS" :key="t.key" type="button" class="tab" :class="{ active: active === t.key }"
            :aria-current="active === t.key ? 'page' : undefined" @click="select(t.key)">
            <component :is="t.icon" :size="18" /> {{ t.label }}
        </button>
    </nav>

    <KeepAlive>
        <VentasReport v-if="active === 'ventas'" />
        <ComprasReport v-else />
    </KeepAlive>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { IconCashRegister, IconFileInvoice, IconReportAnalytics } from '@tabler/icons-vue'
import PageHeader from '../../components/ui/PageHeader.vue'
import VentasReport from './VentasReport.vue'
import ComprasReport from './ComprasReport.vue'

const TABS = [
    { key: 'ventas', label: 'Ventas', icon: IconCashRegister },
    { key: 'compras', label: 'Compras', icon: IconFileInvoice },
]

const route = useRoute()
const router = useRouter()

// La pestaña vive en la URL (?tab=compras) para poder compartir el enlace
const active = computed(() => (TABS.some(t => t.key === route.query.tab) ? route.query.tab : 'ventas'))

function select(key) {
    router.replace({ query: { ...route.query, tab: key } })
}
</script>
