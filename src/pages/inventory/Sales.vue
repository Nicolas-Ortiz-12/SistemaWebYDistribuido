<template>
    <section class="card">
        <div class="card-header">
            <h2 style="margin:0;font-size:18px">Ventas / Facturación</h2>
            <RouterLink class="btn" to="/existencias/stock">Ver stock</RouterLink>
        </div>

        <div class="card-body">
            <!-- Encabezado -->
            <div class="filters" style="margin-bottom:16px">
                <label class="input">
                    <span>Cliente</span>
                    <select v-model.number="sale.customer_id" style="border:none;outline:none;width:100%">
                        <option :value="0">Consumidor Final</option>
                        <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.nombre }}</option>
                    </select>
                </label>
                <label class="input">
                    <span>RUC</span>
                    <input v-model="sale.number" placeholder="000456" />
                </label>
                <label class="input">
                    <span>Fecha</span>
                    <input type="date" v-model="sale.date" />
                </label>
            </div>

            <!-- Detalle -->
            <div class="tabla-container">
                <table>
                    <thead>
                        <tr>
                            <th style="width:35%">Producto</th>
                            <th class="td-right" style="width:15%">Cantidad</th>
                            <th class="td-right" style="width:20%">Precio unit.</th>
                            <th class="td-right" style="width:10%">IVA %</th>
                            <th class="td-right" style="width:20%">Total</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="(it, i) in items" :key="i">
                            <td>
                                <select v-model.number="it.product_id" style="width:100%" @change="seedFromCatalog(i)">
                                    <option :value="0" disabled>Selecciona producto</option>
                                    <option v-for="p in products" :key="p.id" :value="p.id">{{ p.sku }} — {{ p.nombre }}
                                    </option>
                                </select>
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.001" v-model.number="it.qty" style="width:100%" />
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.01" v-model.number="it.unit_price"
                                    style="width:100%" />
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.01" v-model.number="it.tax_rate"
                                    style="width:100%" />
                            </td>
                            <td class="td-right">{{ money(lineTotal(it)) }}</td>
                            <td>
                                <button class="btn eliminar" @click="removeItem(i)" title="Quitar">🗑️</button>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="6" style="padding:10px">
                                <button class="btn" @click="addItem">+ Agregar ítem</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- Totales -->
            <div style="display:flex;justify-content:flex-end;gap:18px;margin-top:12px;flex-wrap:wrap">
                <div>Subtotal: <strong>{{ money(subtotal) }}</strong></div>
                <div>IVA: <strong>{{ money(tax) }}</strong></div>
                <div>Total: <strong>{{ money(total) }}</strong></div>
            </div>

            <!-- Acciones -->
            <div style="display:flex;gap:10px;margin-top:16px">
                <button class="btn" @click="saveDraft">Guardar borrador</button>
                <button class="btn" @click="confirmSale">Confirmar venta</button>
            </div>
        </div>
    </section>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { warehouses } from '../../data/warehouses.js'
import { productsCatalog as products } from '../../data/products_catalog.js'
import { customers } from '../../data/customers.js'

const today = new Date().toISOString().slice(0, 10)
const sale = reactive({ customer_id: 0, warehouse_id: 0, series: '', number: '', date: today })

const items = ref([{ product_id: 0, qty: 0, unit_price: 0, tax_rate: 10 }])

function addItem() { items.value.push({ product_id: 0, qty: 0, unit_price: 0, tax_rate: 10 }) }
function removeItem(i) { items.value.splice(i, 1) }
function seedFromCatalog(i) {
    const it = items.value[i]
    const p = products.find(x => x.id === it.product_id)
    if (p) { it.unit_price = p.price; it.tax_rate = p.tax_rate }
}

const lineTotal = (it) => (it.qty || 0) * (it.unit_price || 0)
const subtotal = computed(() => items.value.reduce((a, it) => a + (it.qty || 0) * (it.unit_price || 0), 0))
const tax = computed(() => items.value.reduce((a, it) => a + ((it.qty || 0) * (it.unit_price || 0)) * (it.tax_rate || 0) / 100, 0))
const total = computed(() => subtotal.value + tax.value)

const money = v => new Intl.NumberFormat('es-PY', { style: 'currency', currency: 'PYG' }).format(v)

function validate() {
    if (!sale.warehouse_id) return alert('Selecciona depósito')
    if (!sale.series || !sale.number) return alert('Serie y número de factura son obligatorios')
    if (!items.value.length) return alert('Agrega al menos un ítem')
    for (const it of items.value) {
        if (!it.product_id) return alert('Selecciona el producto en todos los ítems')
        if ((it.qty || 0) <= 0) return alert('Cantidad debe ser > 0')
        if ((it.unit_price || 0) < 0) return alert('Precio unitario no puede ser negativo')
    }
    return true
}

function saveDraft() { if (validate()) alert('Borrador guardado (mock)') }
function confirmSale() { if (validate()) alert('Venta confirmada: se generarían salidas de stock (mock)') }
</script>