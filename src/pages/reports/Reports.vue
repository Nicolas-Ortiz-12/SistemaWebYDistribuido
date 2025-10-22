<script setup>
import { ref, onMounted } from 'vue'
import '../../assets/reports.css'
import SalesDaily from './tabs/SalesDaily.vue'
import PurchasesReport from './tabs/PurchasesReport.vue' 

const tabs = [
    { key: 'ventasDiarias', label: 'Ventas ' },
    { key: 'comprasDiarias', label: 'Compras' }, 
]

const active = ref(tabs[0].key)

onMounted(() => {
    // Permite abrir con ?tab=comprasDiarias
    const p = new URLSearchParams(location.search)
    const t = p.get('tab')
    if (t && tabs.some(x => x.key === t)) active.value = t
})
</script>

<template>
    <div class="reports">
        <div class="card">
            <div class="tabs">
                <button v-for="t in tabs" :key="t.key" class="tab-btn" :class="{ active: active === t.key }"
                    @click="active = t.key">
                    {{ t.label }}
                </button>
            </div>
        </div>

        <div class="card">
            <SalesDaily v-if="active === 'ventasDiarias'" />
            <PurchasesReport v-else-if="active === 'comprasDiarias'" /> <!-- 👈 -->
        </div>
    </div>
</template>

<style scoped>
.tabs {
    display: flex;
    gap: .5rem;
    flex-wrap: wrap;
    border-bottom: 1px solid #ddd;
    padding-bottom: .25rem;
}

.tab-btn {
    padding: .45rem .8rem;
    cursor: pointer;
    border: none;
    background: transparent;
    border-bottom: 2px solid transparent;
}

.tab-btn.active {
    border-color: #007bff;
    font-weight: 600;
}
</style>
