<template>
    <BaseModal :title="title" :icon="IconAlertTriangle" size="sm" :busy="busy" @close="emit('cancel')">
        <p>{{ message }}</p>
        <template #footer>
            <button type="button" class="btn" :disabled="busy" @click="emit('cancel')">Cancelar</button>
            <button type="button" class="btn btn-danger" :disabled="busy" @click="emit('confirm')">
                <IconLoader2 v-if="busy" :size="16" class="spin" />
                <IconTrash v-else :size="16" />
                {{ confirmLabel }}
            </button>
        </template>
    </BaseModal>
</template>

<script setup>
import { IconAlertTriangle, IconLoader2, IconTrash } from '@tabler/icons-vue'
import BaseModal from './BaseModal.vue'

defineProps({
    title: { type: String, default: 'Confirmar' },
    message: { type: String, required: true },
    confirmLabel: { type: String, default: 'Eliminar' },
    busy: { type: Boolean, default: false },
})
const emit = defineEmits(['confirm', 'cancel'])
</script>
