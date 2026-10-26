<template>
    <div class="field">
        <label class="field-label" :for="id">
            {{ label }}<span v-if="required" class="req" aria-hidden="true">*</span>
        </label>
        <slot :id="id" :invalid="!!error" :describedby="error || hint ? messageId : undefined" />
        <p v-if="error" :id="messageId" class="field-error">{{ error }}</p>
        <p v-else-if="hint" :id="messageId" class="field-hint">{{ hint }}</p>
    </div>
</template>

<script setup>
import { useId } from 'vue'

defineProps({
    label: { type: String, required: true },
    error: { type: String, default: '' },
    hint: { type: String, default: '' },
    required: { type: Boolean, default: false },
})

const id = useId()
const messageId = `${id}-msg`
</script>
