<template>
    <div style="overflow:auto;border:1px solid var(--border);border-radius:12px">
        <table>
            <thead>
                <tr>
                    <th>Depósito</th>
                    <th>codigo</th>
                    <th>Producto</th>
                    <th class="td-right">Stock</th>
                    <th class="td-right">Mínimo</th>
                    <th>Estado</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="r in rows" :key="r.key">
                    <td>{{ r.warehouse }}</td>
                    <td>{{ r.sku }}</td>
                    <td>{{ r.nombre }}</td>
                    <td class="td-right">{{ r.qty }}</td>
                    <td class="td-right">{{ r.min_stock }}</td>
                    <td>
                        <span v-if="r.qty < r.min_stock" class="chip"
                            style="background:#fee2e2;border-color:#fecaca;color:#991b1b">Bajo</span>
                        <span v-else class="chip">OK</span>
                    </td>
                </tr>
                <tr v-if="rows.length === 0">
                    <td colspan="6" style="text-align:center;color:#6b7280;padding:12px">Sin resultados</td>
                </tr>
            </tbody>
        </table>
    </div>
</template>


<script setup>
defineProps({ rows: { type: Array, default: () => [] } })
</script>