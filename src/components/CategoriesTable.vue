<template>
    <div style="overflow:auto;border:1px solid var(--border);border-radius:12px">
        <table>
            <thead>
                <tr>
                    <th>Nombre</th>
                    <th>Editar</th>
                    <th>Eliminar</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="c in rows" :key="c.nombre">
                    <td><a href="#" class="chip">{{ c.nombre }}</a></td>
                    <td>
                        <button class="icon-btn" title="Editar" @click="$emit('edit', c)">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                                <path d="M12 20h9" />
                                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                            </svg>
                        </button>
                    </td>
                    <td>
                        <button class="icon-btn danger" title="Eliminar" @click="$emit('remove', c)">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                                <polyline points="3 6 5 6 21 6" />
                                <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                                <path d="M10 11v6" />
                                <path d="M14 11v6" />
                                <path d="M9 6V4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2" />
                            </svg>
                        </button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<script setup>
defineProps({ rows: { type: Array, default: () => [] } });
</script>
