<template>
    <div class="toast-host" aria-live="polite">
        <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type" :role="t.type === 'error' ? 'alert' : 'status'">
            <IconCircleCheck v-if="t.type === 'success'" :size="20" />
            <IconAlertCircle v-else :size="20" />
            <span class="toast-text">{{ t.message }}</span>
            <button type="button" class="icon-btn" aria-label="Cerrar aviso" @click="dismissToast(t.id)">
                <IconX :size="16" />
            </button>
        </div>
    </div>
</template>

<script setup>
import { IconAlertCircle, IconCircleCheck, IconX } from '@tabler/icons-vue'
import { dismissToast, toasts } from '../../composables/useToast'
</script>
