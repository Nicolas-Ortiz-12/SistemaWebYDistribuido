<template>
    <div class="empty-state">
        <span class="empty-state-icon"><component :is="icon" :size="24" /></span>
        <p class="empty-state-title">{{ title }}</p>
        <p v-if="text">{{ text }}</p>
        <slot />
    </div>
</template>

<script setup>
import { IconInbox } from '@tabler/icons-vue'

defineProps({
    title: { type: String, required: true },
    text: { type: String, default: '' },
    icon: { type: [Object, Function], default: () => IconInbox },
})
</script>
