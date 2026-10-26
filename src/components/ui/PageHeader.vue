<template>
    <header class="page-header">
        <div class="page-header-title">
            <span v-if="icon" class="page-header-icon"><component :is="icon" :size="22" /></span>
            <div>
                <h1>{{ title }}</h1>
                <p v-if="subtitle" class="page-header-subtitle">{{ subtitle }}</p>
            </div>
        </div>
        <div v-if="$slots.default" class="row">
            <slot />
        </div>
    </header>
</template>

<script setup>
defineProps({
    title: { type: String, required: true },
    subtitle: { type: String, default: '' },
    icon: { type: [Object, Function], default: null },
})
</script>
