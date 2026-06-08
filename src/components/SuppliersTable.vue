<template>
  <table class="data-table">
    <thead>
      <tr>
        <th>Nombre</th>
        <th>RUC</th>
        <th>Telefono</th>
        <th>Correo</th>
        <th>Direccion</th>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="row in rows" :key="row.id ?? row.ruc ?? row.nombre">
        <td>{{ row.nombre }}</td>
        <td>{{ row.ruc }}</td>
        <td>{{ row.telefono }}</td>
        <td>{{ row.correo }}</td>
        <td>{{ row.direccion }}</td>
        <td class="row-actions">
          <button type="button" class="mini-btn" @click="$emit('edit', row)">Editar</button>
          <button type="button" class="mini-btn danger" @click="$emit('remove', row)">Eliminar</button>
        </td>
      </tr>
      <tr v-if="!rows?.length">
        <td colspan="6" class="empty">Sin registros</td>
      </tr>
    </tbody>
  </table>
</template>

<script setup>
defineProps({
  rows: { type: Array, default: () => [] },
});

defineEmits(["edit", "remove"]);
</script>
