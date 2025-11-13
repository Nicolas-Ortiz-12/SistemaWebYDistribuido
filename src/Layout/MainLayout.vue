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
import SideNav from '../components/SideNav.vue'
import TopBar from '../components/TopBar.vue'
</script>
