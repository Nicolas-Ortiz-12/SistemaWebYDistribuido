<template>
    <div class="alert" :class="`alert-${type}`" :role="type === 'error' ? 'alert' : 'status'">
        <component :is="ICONS[type]" :size="18" />
        <div class="alert-body"><slot /></div>
    </div>
</template>

<script setup>
import { IconAlertCircle, IconAlertTriangle, IconCircleCheck, IconInfoCircle } from '@tabler/icons-vue'

defineProps({
    type: { type: String, default: 'info', validator: v => ['info', 'warning', 'error', 'success'].includes(v) },
})

const ICONS = { info: IconInfoCircle, warning: IconAlertTriangle, error: IconAlertCircle, success: IconCircleCheck }
</script>
