<template>
    <div style="overflow:auto;border:1px solid var(--border);border-radius:12px">
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>RUC</th>
                    <th>Teléfono</th>
                    <th>Correo electrónico</th>
                    <th>Dirección</th>
                    <th>Editar</th>
                </tr>
            </thead>

            <tbody>
                <tr v-for="c in rows" :key="c.id">
                    <td>{{ c.id }}</td>
                    <td>{{ c.nombre }}</td>
                    <td>{{ c.ruc }}</td>
                    <td>{{ c.telefono }}</td>
                    <td>{{ c.correo }}</td>
                    <td>{{ c.direccion }}</td>
                    <td>
                        <button class="icon-btn" title="Editar" @click="$emit('edit', c)">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                                <path d="M12 20h9" />
                                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                            </svg>
                        </button>
                    </td>
                </tr>

                <!-- Mostrar si no hay registros -->
                <tr v-if="!rows || rows.length === 0">
                    <td colspan="7" style="text-align:center;color:var(--muted);padding:16px;">
                        No hay clientes registrados.
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<script setup>
defineProps({
    rows: { type: Array, default: () => [] }
})
</script>
