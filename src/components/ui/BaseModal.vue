<template>
    <Teleport to="body">
        <div class="modal-root" @keydown.esc.stop="close">
            <div class="modal-backdrop" @click="close" />
            <section ref="panel" class="modal" :class="`size-${size}`" role="dialog" aria-modal="true"
                :aria-labelledby="titleId" tabindex="-1">
                <header class="card-header">
                    <h2 :id="titleId" class="card-title">
                        <component :is="icon" v-if="icon" :size="18" />
                        {{ title }}
                    </h2>
                    <button type="button" class="icon-btn" aria-label="Cerrar" :disabled="busy" @click="close">
                        <IconX :size="18" />
                    </button>
                </header>
                <div class="modal-body">
                    <slot />
                </div>
                <footer v-if="$slots.footer" class="card-footer">
                    <slot name="footer" />
                </footer>
            </section>
        </div>
    </Teleport>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, useId } from 'vue'
import { IconX } from '@tabler/icons-vue'

const props = defineProps({
    title: { type: String, required: true },
    icon: { type: [Object, Function], default: null },
    size: { type: String, default: 'md' },
    // Mientras se guarda no se puede cerrar
    busy: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])

const titleId = useId()
const panel = ref(null)
let previousFocus = null

function close() {
    if (!props.busy) emit('close')
}

onMounted(async () => {
    previousFocus = document.activeElement
    document.body.classList.add('modal-open')
    await nextTick()
    const first = panel.value?.querySelector('input:not([disabled]), select:not([disabled]), textarea:not([disabled])')
    ;(first ?? panel.value)?.focus()
})

onUnmounted(() => {
    document.body.classList.remove('modal-open')
    previousFocus?.focus?.()
})
</script>
