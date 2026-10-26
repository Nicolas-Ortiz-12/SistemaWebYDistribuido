<template>
    <header class="topbar">
        <button type="button" class="icon-btn topbar-menu" aria-label="Abrir menú" @click="emit('toggle-menu')">
            <IconMenu2 :size="20" />
        </button>
        <span class="topbar-title">{{ title }}</span>
        <span class="spacer" />
        <div v-if="currentUser" class="user-chip">
            <span class="avatar" aria-hidden="true">{{ initial }}</span>
            <span class="user-chip-text user-chip-name">{{ currentUser.username }}</span>
            <button type="button" class="btn btn-ghost btn-sm" @click="onLogout">
                <IconLogout :size="16" />
                Salir
            </button>
        </div>
    </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { IconLogout, IconMenu2 } from '@tabler/icons-vue'
import { currentUser, logout } from '../../auth/session'

const emit = defineEmits(['toggle-menu'])
const route = useRoute()
const router = useRouter()

const title = computed(() => route.meta.title ?? '')
const initial = computed(() => currentUser.value?.username.charAt(0).toUpperCase() ?? '?')

function onLogout() {
    logout()
    router.replace({ name: 'login' })
}
</script>
