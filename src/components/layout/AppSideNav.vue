<template>
    <div class="sidebar-backdrop" :class="{ open }" @click="emit('close')" />
    <aside class="sidebar" :class="{ open }" aria-label="Navegación principal">
        <div class="sidebar-brand">
            <span class="sidebar-brand-icon"><IconTool :size="20" /></span>
            {{ APP_NAME }}
        </div>

        <nav class="sidebar-nav">
            <template v-for="section in sections" :key="section.title">
                <p class="sidebar-section">{{ section.title }}</p>
                <RouterLink v-for="item in section.items" :key="item.to" :to="item.to" class="sidebar-link"
                    @click="emit('close')">
                    <component :is="item.icon" :size="19" />
                    {{ item.label }}
                </RouterLink>
            </template>
        </nav>
    </aside>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import {
    IconCashRegister, IconCategory, IconChartBar, IconFileInvoice, IconLayoutDashboard,
    IconPackage, IconTool, IconTruck, IconUsers,
} from '@tabler/icons-vue'
import { APP_NAME } from '../../config'

defineProps({ open: { type: Boolean, default: false } })
const emit = defineEmits(['close'])

const sections = [
    { title: 'General', items: [{ to: '/', label: 'Dashboard', icon: IconLayoutDashboard }] },
    {
        title: 'Operaciones',
        items: [
            { to: '/existencias/ventas', label: 'Nueva venta', icon: IconCashRegister },
            { to: '/existencias/compras', label: 'Ingresar compra', icon: IconFileInvoice },
        ],
    },
    {
        title: 'Catálogo',
        items: [
            { to: '/productos/lista', label: 'Productos', icon: IconPackage },
            { to: '/productos/categorias', label: 'Categorías', icon: IconCategory },
            { to: '/productos/proveedores', label: 'Proveedores', icon: IconTruck },
            { to: '/clientes', label: 'Clientes', icon: IconUsers },
        ],
    },
    { title: 'Análisis', items: [{ to: '/reportes', label: 'Reportes', icon: IconChartBar }] },
]
</script>
