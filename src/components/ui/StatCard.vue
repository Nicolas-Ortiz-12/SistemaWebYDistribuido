<template>
    <article class="card stat-card">
        <span class="stat-icon" :class="tone && `tone-${tone}`"><component :is="icon" :size="22" /></span>
        <div>
            <p class="stat-label">{{ label }}</p>
            <p class="stat-value">{{ loading ? '…' : value }}</p>
            <p v-if="hint" class="stat-hint">{{ hint }}</p>
        </div>
    </article>
</template>

<script setup>
defineProps({
    icon: { type: [Object, Function], required: true },
    label: { type: String, required: true },
    value: { type: [String, Number], default: '—' },
    hint: { type: String, default: '' },
    tone: { type: String, default: '' },
    loading: { type: Boolean, default: false },
})
</script>
