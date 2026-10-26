<template>
    <div class="combobox">
        <input :id="inputId" ref="input" v-model="query" class="input" :class="{ invalid }" type="text" role="combobox"
            autocomplete="off" spellcheck="false" :maxlength="LIMITS.busqueda" :placeholder="placeholder"
            :disabled="disabled" :aria-expanded="open" :aria-controls="listId" aria-autocomplete="list"
            :aria-activedescendant="open && options[active] ? `${listId}-${active}` : undefined" @focus="onFocus" @click="onClick"
            @blur="onBlur" @input="onInput" @keydown.down.prevent="move(1)" @keydown.up.prevent="move(-1)"
            @keydown.enter="onEnter" @keydown.esc="open = false" />
        <button v-if="modelValue != null && !disabled" type="button" class="icon-btn combobox-clear"
            aria-label="Quitar selección" @mousedown.prevent @click="clear">
            <IconX :size="16" />
        </button>

        <ul v-if="open" :id="listId" class="combobox-list" role="listbox">
            <li v-if="loading" class="combobox-status">Buscando…</li>
            <li v-else-if="error" class="combobox-status">{{ error }}</li>
            <li v-else-if="options.length === 0" class="combobox-status">Sin resultados</li>
            <template v-else>
                <li v-for="(opt, i) in options" :id="`${listId}-${i}`" :key="opt.id" class="combobox-option"
                    :class="{ active: i === active }" role="option" :aria-selected="opt.id === modelValue"
                    @mousedown.prevent="choose(opt)" @mousemove="active = i">
                    <span>{{ opt.label }}</span>
                    <span v-if="opt.hint" class="combobox-option-hint">{{ opt.hint }}</span>
                </li>
            </template>
        </ul>
    </div>
</template>

<script setup>
import { onUnmounted, ref, useId, watch } from 'vue'
import { IconX } from '@tabler/icons-vue'
import { LIMITS } from '../../utils/validation'

const props = defineProps({
    modelValue: { type: [Number, String], default: null },
    // Texto de la opción elegida (el componente solo conoce el id)
    selectedLabel: { type: String, default: '' },
    // (texto, AbortSignal) => Promise<[{ id, label, hint?, data? }]>
    search: { type: Function, required: true },
    placeholder: { type: String, default: 'Buscar…' },
    inputId: { type: String, default: undefined },
    disabled: { type: Boolean, default: false },
    invalid: { type: Boolean, default: false },
    // Modo "agregar": al elegir una opción el campo se vacía y queda listo para la siguiente
    clearOnSelect: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'select'])

const listId = useId()
const input = ref(null)
const query = ref(props.selectedLabel)
const open = ref(false)
const options = ref([])
const active = ref(0)
const loading = ref(false)
const error = ref('')

let controller = null
let timer = null

watch(() => props.selectedLabel, label => {
    if (document.activeElement !== input.value) query.value = label
})

async function runSearch(text) {
    controller?.abort()
    const current = (controller = new AbortController())
    loading.value = true
    error.value = ''
    try {
        options.value = await props.search(text.trim(), current.signal)
        active.value = 0
    } catch (e) {
        if (e.name !== 'AbortError') error.value = e.message
    } finally {
        if (controller === current) loading.value = false
    }
}

function onFocus() {
    open.value = true
    // Con algo ya elegido se listan todas las opciones para poder cambiarlo
    runSearch(props.modelValue != null ? '' : query.value)
}

// Con el campo ya enfocado y la lista cerrada, un clic vuelve a abrirla
function onClick() {
    if (!open.value) onFocus()
}

function onInput() {
    open.value = true
    // Escribir encima de una selección la descarta: el texto ya no coincide con el id
    if (props.modelValue != null) {
        emit('update:modelValue', null)
        emit('select', null)
    }
    clearTimeout(timer)
    timer = setTimeout(() => runSearch(query.value), 250)
}

function onBlur() {
    open.value = false
    query.value = props.modelValue != null ? props.selectedLabel : ''
}

function move(delta) {
    if (!open.value) return onFocus()
    if (options.value.length) active.value = (active.value + delta + options.value.length) % options.value.length
}

function onEnter(event) {
    if (!open.value) return
    event.preventDefault()
    const opt = options.value[active.value]
    if (opt) choose(opt)
}

function choose(opt) {
    emit('select', opt)
    open.value = false
    if (props.clearOnSelect) {
        query.value = ''
        return
    }
    emit('update:modelValue', opt.id)
    query.value = opt.label
}

function clear() {
    emit('update:modelValue', null)
    emit('select', null)
    query.value = ''
    input.value?.focus()
}

onUnmounted(() => {
    controller?.abort()
    clearTimeout(timer)
})
</script>
