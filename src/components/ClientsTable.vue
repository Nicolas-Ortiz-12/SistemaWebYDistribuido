<template>
    <div style="overflow:auto;border:1px solid var(--border);border-radius:12px">
        <table>
            <thead>
                <tr>
                    <th>Nombre</th>
                    <th>Correo electrónico</th>
                    <th>Teléfono</th>
                    <th>Dirección</th>
                    <th class="td-right">Importe comprado</th>
                    <th class="td-right">Importe pagado</th>
                    <th class="td-right">Importe debido</th>
                    <th>Editar</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="c in rows" :key="c.email">
                    <td><a href="#" class="chip">{{ c.nombre }}</a></td>
                    <td>{{ c.email }}</td>
                    <td>{{ c.telefono }}</td>
                    <td>{{ c.direccion }}</td>
                    <td class="td-right">{{ money(c.comprado) }}</td>
                    <td class="td-right">{{ money(c.pagado) }}</td>
                    <td class="td-right">{{ money(c.comprado - c.pagado) }}</td>
                    <td>
                        <button class="icon-btn" title="Editar" @click="$emit('edit', c)">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                                <path d="M12 20h9" />
                                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                            </svg>
                        </button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</template>


<script setup>
defineProps({ rows: { type: Array, default: () => [] }, money: { type: Function, required: true } })
</script>