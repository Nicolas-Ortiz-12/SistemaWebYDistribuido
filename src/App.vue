<template>
    <RouterView v-if="route.meta.public" />
    <div v-else class="app-shell">
        <AppSideNav :open="menuOpen" @close="menuOpen = false" />
        <div class="app-main">
            <AppTopBar @toggle-menu="menuOpen = !menuOpen" />
            <main class="app-content">
                <RouterView />
            </main>
        </div>
    </div>
    <ToastHost />
</template>

<script setup>
import { ref } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppSideNav from './components/layout/AppSideNav.vue'
import AppTopBar from './components/layout/AppTopBar.vue'
import ToastHost from './components/ui/ToastHost.vue'

const route = useRoute()
const menuOpen = ref(false)
</script>
