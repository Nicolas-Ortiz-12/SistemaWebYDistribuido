<template>
    <div class="lines-box">
        <div class="lines-search">
            <label class="sr-only" for="buscar-producto">Agregar producto</label>
            <div class="input-icon">
                <IconSearch :size="18" />
                <SearchSelect :model-value="null" input-id="buscar-producto" :search="searchProductos" clear-on-select
                    placeholder="Buscá un producto por nombre o código y elegilo para agregarlo…"
                    @select="onAdd" />
            </div>
        </div>

        <EmptyState v-if="!doc.lines.value.length" :icon="IconShoppingCartPlus" title="Todavía no agregaste productos"
            text="Escribí arriba el nombre o el código y elegí el producto: se suma a esta lista." />

        <template v-else>
            <div class="lines-header" aria-hidden="true">
                <span>Producto</span>
                <span>Cantidad</span>
                <span>{{ priceEditable ? 'Costo unit.' : 'Precio unit.' }}</span>
                <span>IVA</span>
                <span class="text-right">Total</span>
                <span />
            </div>
            <ul class="lines">
                <li v-for="line in doc.lines.value" :key="line.key" class="line">
                    <div class="line-name">
                        <span class="cell-main">{{ line.label }}</span>
                        <span v-if="line.stock != null" class="cell-sub" :class="{ 'field-error': overStock(line) }">
                            Stock disponible: {{ formatNumber(line.stock) }} {{ line.unidad }}
                        </span>
                    </div>
                    <label class="line-cell" data-label="Cantidad">
                        <span class="sr-only">Cantidad de {{ line.label }}</span>
                        <input v-model.number="line.cantidad" class="input input-sm" :class="{ invalid: overStock(line) }"
                            type="number" min="0" step="any" inputmode="decimal" />
                    </label>
                    <label class="line-cell" :data-label="priceEditable ? 'Costo unit.' : 'Precio unit.'">
                        <span class="sr-only">{{ priceEditable ? 'Costo unitario' : 'Precio unitario' }} de {{ line.label }}</span>
                        <input v-if="priceEditable" v-model.number="line.precio" class="input input-sm" type="number"
                            min="0" step="any" inputmode="decimal" />
                        <span v-else class="line-static">{{ formatMoney(line.precio) }}</span>
                    </label>
                    <label class="line-cell" data-label="IVA">
                        <span class="sr-only">IVA de {{ line.label }}</span>
                        <select v-model.number="line.tasaIva" class="select input-sm">
                            <option v-for="rate in IVA_RATES" :key="rate" :value="rate">{{ rate }} %</option>
                        </select>
                    </label>
                    <span class="line-cell line-total" data-label="Total">{{ formatMoney(lineSubtotal(line) + lineIva(line)) }}</span>
                    <button type="button" class="icon-btn danger" :aria-label="`Quitar ${line.label}`" title="Quitar"
                        @click="doc.remove(line.key)">
                        <IconTrash :size="18" />
                    </button>
                </li>
            </ul>
        </template>
    </div>
</template>

<script setup>
import { IconSearch, IconShoppingCartPlus, IconTrash } from '@tabler/icons-vue'
import SearchSelect from './ui/SearchSelect.vue'
import EmptyState from './ui/EmptyState.vue'
import { searchProductos } from '../api/searchers'
import { IVA_RATES, lineIva, lineSubtotal } from '../composables/useDocumentLines'
import { formatMoney, formatNumber } from '../utils/format'

const props = defineProps({
    // Instancia de useDocumentLines()
    doc: { type: Object, required: true },
    // Compra: el costo se carga a mano. Venta: el backend usa siempre el precio de lista.
    priceEditable: { type: Boolean, default: false },
    checkStock: { type: Boolean, default: false },
})

// Un producto ya cargado no se duplica: se le suma una unidad
function onAdd(option) {
    if (option) props.doc.addProduct(option, { useCost: props.priceEditable })
}

const overStock = line => props.checkStock && line.stock != null && Number(line.cantidad) > line.stock
</script>
