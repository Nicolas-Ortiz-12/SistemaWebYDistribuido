<template>
  <div class="pager">
    <button type="button" class="btn btn-ghost" :disabled="page <= 1" @click="page = page - 1">Anterior</button>
    <span>Pagina {{ page }} / {{ pages }}</span>
    <button type="button" class="btn btn-ghost" :disabled="page >= pages" @click="page = page + 1">Siguiente</button>
    <select v-model.number="pageSize" class="page-size">
      <option :value="10">10</option>
      <option :value="20">20</option>
      <option :value="50">50</option>
    </select>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
});

const emit = defineEmits(["update:page", "update:pageSize"]);

const pages = computed(() => {
  const safePageSize = props.pageSize > 0 ? props.pageSize : 1;
  return Math.max(1, Math.ceil(props.total / safePageSize));
});

const page = computed({
  get: () => props.page,
  set: (value) => emit("update:page", value),
});

const pageSize = computed({
  get: () => props.pageSize,
  set: (value) => emit("update:pageSize", value),
});
</script>
