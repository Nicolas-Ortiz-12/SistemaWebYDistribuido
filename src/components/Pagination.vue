<template>
  <div class="pager">
    <div class="pager-left">
      <span>{{ rangeStart }}–{{ rangeEnd }} de {{ total }}</span>
      <label class="pager-size">
        Por página:
        <select :value="pageSize" @change="onSize($event)">
          <option v-for="s in sizes" :key="s" :value="s">{{ s }}</option>
        </select>
      </label>
    </div>

    <div class="pager-right">
      <button class="btn" :disabled="page <= 1" @click="goFirst">«</button>
      <button class="btn" :disabled="page <= 1" @click="goPrev">‹</button>
      <span class="page-indicator">Página {{ page }} / {{ pageCount }}</span>
      <button class="btn" :disabled="page >= pageCount" @click="goNext">›</button>
      <button class="btn" :disabled="page >= pageCount" @click="goLast">»</button>
    </div>
  </div>
</template>

<script setup>
import "../assets/pagination.css"
import { computed } from 'vue'

const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  sizes: { type: Array, default: () => [5, 10, 20, 50] }
})
const emit = defineEmits(['update:page', 'update:pageSize'])

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const rangeStart = computed(() => props.total === 0 ? 0 : (props.page - 1) * props.pageSize + 1)
const rangeEnd = computed(() => Math.min(props.total, props.page * props.pageSize))

function goFirst() { emit('update:page', 1) }
function goPrev() { emit('update:page', Math.max(1, props.page - 1)) }
function goNext() { emit('update:page', Math.min(pageCount.value, props.page + 1)) }
function goLast() { emit('update:page', pageCount.value) }
function onSize(e) {
  const newSize = Number(e.target.value)
  emit('update:pageSize', newSize)
  emit('update:page', 1) // vuelve al inicio al cambiar tamaño
}
</script>

