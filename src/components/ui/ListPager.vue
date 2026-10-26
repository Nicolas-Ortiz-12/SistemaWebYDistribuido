<template>
    <div class="pager">
        <span>{{ rangeText }}</span>
        <div class="row">
            <label class="row">
                <span>Por página</span>
                <select class="select" :value="pageSize" @change="emit('update:pageSize', Number($event.target.value))">
                    <option v-for="s in sizes" :key="s" :value="s">{{ s }}</option>
                </select>
            </label>
            <div class="pager-controls">
                <button type="button" class="icon-btn" aria-label="Primera página" :disabled="page <= 0" @click="go(0)">
                    <IconChevronsLeft :size="18" />
                </button>
                <button type="button" class="icon-btn" aria-label="Página anterior" :disabled="page <= 0" @click="go(page - 1)">
                    <IconChevronLeft :size="18" />
                </button>
                <span class="pager-page">{{ page + 1 }} / {{ pageCount }}</span>
                <button type="button" class="icon-btn" aria-label="Página siguiente" :disabled="page >= pageCount - 1"
                    @click="go(page + 1)">
                    <IconChevronRight :size="18" />
                </button>
                <button type="button" class="icon-btn" aria-label="Última página" :disabled="page >= pageCount - 1"
                    @click="go(pageCount - 1)">
                    <IconChevronsRight :size="18" />
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import { IconChevronLeft, IconChevronRight, IconChevronsLeft, IconChevronsRight } from '@tabler/icons-vue'
import { formatNumber } from '../../utils/format'

// page es 0-based, como en el backend
const props = defineProps({
    page: { type: Number, required: true },
    pageSize: { type: Number, required: true },
    total: { type: Number, required: true },
    sizes: { type: Array, default: () => [10, 20, 50] },
})
const emit = defineEmits(['update:page', 'update:pageSize'])

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const rangeText = computed(() => {
    if (props.total === 0) return 'Sin registros'
    const from = props.page * props.pageSize + 1
    const to = Math.min(props.total, (props.page + 1) * props.pageSize)
    return `${formatNumber(from)}–${formatNumber(to)} de ${formatNumber(props.total)}`
})

function go(p) {
    emit('update:page', Math.min(Math.max(0, p), pageCount.value - 1))
}
</script>
