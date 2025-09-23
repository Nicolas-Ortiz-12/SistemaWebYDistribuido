<template>
    <section class="card">
        <div class="card-header">
            <h2 style="margin:0;font-size:18px">Nueva compra (ingreso por factura)</h2>
            <RouterLink class="btn" to="/productos/lista">Ver stock</RouterLink>
        </div>

        <div class="card-body">
            <!-- Encabezado -->
            <div class="filters" style="margin-bottom:16px">
                <label class="input">
                    <span style="white-space:nowrap">Proveedor</span>
                    <select v-model.number="form.supplier_id" style="border:none;outline:none;width:100%">
                        <option :value="0" disabled>Selecciona proveedor</option>
                        <option v-for="(s, idx) in suppliersWithId" :key="s.id" :value="s.id">{{ s.nombre }}</option>
                    </select>
                </label>
                <label class="input">
                    <span>Serie</span>
                    <input v-model="form.series" placeholder="F001" />
                </label>
                <label class="input">
                    <span>Número</span>
                    <input v-model="form.number" placeholder="000123" />
                </label>
                <label class="input">
                    <span>Fecha</span>
                    <input type="date" v-model="form.issue_date" />
                </label>
            </div>

            <!-- Detalle -->
            <div class="tabla-container">
                <table>
                    <thead>
                        <tr>
                            <th style="width:35%">Producto</th>
                            <th class="td-right" style="width:15%">Cantidad</th>
                            <th class="td-right" style="width:20%">Costo unit.</th>
                            <th class="td-right" style="width:10%">IVA %</th>
                            <th class="td-right" style="width:20%">Total</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="(it, i) in items" :key="i">
                            <td>
                                <select v-model.number="it.product_id" style="width:100%">
                                    <option :value="0" disabled>Selecciona producto</option>
                                    <option v-for="p in products" :key="p.id" :value="p.id">{{ p.sku }} — {{ p.nombre }}
                                    </option>
                                </select>
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.001" v-model.number="it.qty" style="width:100%" />
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.01" v-model.number="it.unit_cost"
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
                <button class="btn" @click="confirmPurchase">Confirmar ingreso</button>
            </div>
        </div>
    </section>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { warehouses } from '../../data/warehouses.js'
import { productsCatalog as products } from '../../data/products_catalog.js'
import { suppliers as suppliersRaw } from '../../data/suppliers.js'

const today = new Date().toISOString().slice(0, 10)
const form = reactive({ supplier_id: 0, warehouse_id: 0, series: '', number: '', issue_date: today })

// añade ids a los proveedores si tu dataset anterior no los tenía
const suppliersWithId = suppliersRaw.map((s, i) => ({ id: i + 1, nombre: s.nombre || s.name || s.email || `Proveedor ${i + 1}` }))

const items = ref([{ product_id: 0, qty: 0, unit_cost: 0, tax_rate: 10 }])

function addItem() { items.value.push({ product_id: 0, qty: 0, unit_cost: 0, tax_rate: 10 }) }
function removeItem(i) { items.value.splice(i, 1) }

const lineTotal = (it) => (it.qty || 0) * (it.unit_cost || 0) - 0 // sin descuento
const subtotal = computed(() => items.value.reduce((a, it) => a + (it.qty || 0) * (it.unit_cost || 0), 0))
const tax = computed(() => items.value.reduce((a, it) => a + ((it.qty || 0) * (it.unit_cost || 0)) * (it.tax_rate || 0) / 100, 0))
const total = computed(() => subtotal.value + tax.value)

const money = v => new Intl.NumberFormat('es-PY', { style: 'currency', currency: 'PYG' }).format(v)

function validate() {
    if (!form.supplier_id) return alert('Selecciona un proveedor')
    if (!form.warehouse_id) return alert('Selecciona un depósito')
    if (!form.series || !form.number) return alert('Serie y número de factura son obligatorios')
    if (!items.value.length) return alert('Agrega al menos un ítem')
    for (const it of items.value) {
        if (!it.product_id) return alert('Selecciona el producto en todos los ítems')
        if ((it.qty || 0) <= 0) return alert('Cantidad debe ser > 0')
        if ((it.unit_cost || 0) < 0) return alert('Costo unitario no puede ser negativo')
    }
    return true
}

function saveDraft() { if (validate()) alert('Borrador guardado (mock)') }
function confirmPurchase() { if (validate()) alert('Compra confirmada: se generarían entradas de stock (mock)') }
</script>