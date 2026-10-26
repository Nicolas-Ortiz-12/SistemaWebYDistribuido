<template>
    <section class="card">
        <EmptyState :icon="IconError404" title="Página no encontrada"
            text="La dirección no existe o fue movida. Revisá el enlace o volvé al inicio.">
            <RouterLink class="btn btn-primary" to="/"><IconHome :size="18" /> Ir al dashboard</RouterLink>
        </EmptyState>
    </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import { IconError404, IconHome } from '@tabler/icons-vue'
import EmptyState from '../components/ui/EmptyState.vue'
</script>
