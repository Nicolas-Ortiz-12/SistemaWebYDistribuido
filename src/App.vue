<template>
    <div>
        <TopBar />
        <div class="layout">
            <SideNav />
            <main>
                <RouterView />
            </main>
        </div>
    </div>
</template>


<script setup>
import { RouterView } from 'vue-router'
import TopBar from './components/TopBar.vue'
import SideNav from './components/SideNav.vue'
</script>