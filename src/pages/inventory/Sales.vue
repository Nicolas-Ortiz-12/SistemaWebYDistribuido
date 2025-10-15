<template>
    <section class="card">
        <div class="card-header">
            <h2 style="margin:0;font-size:18px">Ventas / Facturación</h2>
            <RouterLink class="btn" to="/pages/products/Products">Ver stock</RouterLink>
        </div>

        <div class="card-body">
            <!-- Encabezado -->
            <div class="filters"
                style="margin-bottom:16px; display:grid; grid-template-columns: 1fr 180px 180px; gap:12px">
                <label class="input">
                    <span>Cliente</span>
                    <select v-model.number="sale.customer_id" style="border:none;outline:none;width:100%">
                        <option :value="0">Consumidor Final</option>
                        <option v-for="c in customers" :key="c.id" :value="c.id">
                            {{ c.nombre }}
                        </option>
                    </select>
                </label>

                <label class="input">
                    <span>Número</span>
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
                                    <option v-for="p in products" :key="p.id" :value="p.id">
                                        {{ p.codigo || p.sku || p.id }} — {{ p.nombre }}
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
                <button class="btn ghost" @click="saveDraft" :disabled="saving || loading">Guardar borrador</button>
                <button class="btn" @click="confirmSale" :disabled="saving || loading">
                    <span v-if="!saving">Confirmar venta</span>
                    <span v-else>Enviando…</span>
                </button>
            </div>

            <div v-if="loading" class="loading" style="margin-top:10px">Cargando catálogos…</div>
            <div v-if="error" class="error" style="margin-top:10px">{{ error }}</div>
            <div v-if="ok" class="chip" style="margin-top:10px">✅ Venta registrada correctamente</div>
        </div>
    </section>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'

// URL base del backend (evita dobles "/")
const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '')

// Estado venta
const today = new Date().toISOString().slice(0, 10)
const sale = reactive({ customer_id: 0, series: '', number: '', date: today })

// Catálogos desde API
const customers = ref([])
const products = ref([])

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const ok = ref(false)

// Ítems del detalle (precio solo para UI)
const items = ref([{ product_id: 0, qty: 0, unit_price: 0, tax_rate: 10 }])

function addItem() { items.value.push({ product_id: 0, qty: 0, unit_price: 0, tax_rate: 10 }) }
function removeItem(i) { items.value.splice(i, 1) }

function seedFromCatalog(i) {
    const it = items.value[i]
    const p = products.value.find(x => x.id === it.product_id)
    if (p) {
        if (typeof p.precio === 'number') it.unit_price = p.precio
        if (typeof p.tasaIva === 'number') it.tax_rate = p.tasaIva
    }
}

// Totales (solo UI)
const lineTotal = (it) => (it.qty || 0) * (it.unit_price || 0)
const subtotal = computed(() => items.value.reduce((a, it) => a + (it.qty || 0) * (it.unit_price || 0), 0))
const tax = computed(() => items.value.reduce((a, it) => a + ((it.qty || 0) * (it.unit_price || 0)) * (it.tax_rate || 0) / 100, 0))
const total = computed(() => subtotal.value + tax.value)
const money = v => new Intl.NumberFormat('es-PY', { style: 'currency', currency: 'PYG' }).format(v)

// Carga catálogos (usa tus endpoints con path variables)
async function fetchClientes() {
    const resp = await fetch(`${API_URL}/clientes/0/100`)
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
    const data = await resp.json()
    customers.value = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : [])
}

async function fetchProductos() {
    const resp = await fetch(`${API_URL}/productos/0/200`)
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
    const data = await resp.json()
    products.value = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : [])
}

onMounted(async () => {
    error.value = ''
    loading.value = true
    try {
        await Promise.all([fetchClientes(), fetchProductos()])
    } catch (e) {
        console.error(e)
        error.value = 'No se pudieron cargar clientes/productos.'
    } finally {
        loading.value = false
    }
})

// Validación básica
function validate() {
    if (!sale.number?.trim()) { alert('Número de factura es obligatorio'); return false }
    if (!sale.date) { alert('Fecha obligatoria'); return false }
    if (!items.value.length) { alert('Agrega al menos un ítem'); return false }
    for (const it of items.value) {
        if (!it.product_id) { alert('Selecciona producto en todos los ítems'); return false }
        if ((it.qty || 0) <= 0) { alert('Cantidad debe ser > 0'); return false }
        if ((it.tax_rate || 0) < 0) { alert('IVA inválido'); return false }
    }
    return true
}

// Borrador local (si luego tenés endpoint /ventas/borrador, lo adaptamos)
function saveDraft() {
    if (!validate()) return
    ok.value = false
    error.value = ''
    alert('Borrador guardado localmente. Si tenés endpoint para borradores, lo apunto ahí.')
}

// POST /ventas con el payload requerido por tu venta-controller
async function confirmSale() {
    if (!validate()) return
    ok.value = false
    error.value = ''
    saving.value = true

    try {
        const payload = {
            clienteId: sale.customer_id || null,
            numero: sale.number?.trim() || null,
            // del input date (YYYY-MM-DD) a ISO (Z)
            fechaVenta: new Date(sale.date).toISOString(),

            // totales (por si el servicio los acepta; si los recalcula, no molesta)
            subtotal: Number(subtotal.value.toFixed(2)),
            iva: Number(tax.value.toFixed(2)),
            total: Number(total.value.toFixed(2)),

            // detalle (ajusta claves si tu DTO difiere)
            items: items.value.map(it => ({
                productoId: it.product_id,
                cantidad: it.qty,
                precioUnitario: it.unit_price,
                tasaIva: it.tax_rate
            }))
        }

        const resp = await fetch(`${API_URL}/ventas`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })

        if (!resp.ok) {
            let msg = `Error HTTP ${resp.status}`
            try { const j = await resp.json(); msg = j?.message || msg } catch { }
            throw new Error(msg)
        }

        ok.value = true
        // reset
        sale.customer_id = 0
        sale.number = ''
        sale.date = new Date().toISOString().slice(0, 10)
        items.value = [{ product_id: 0, qty: 0, unit_price: 0, tax_rate: 10 }]
    } catch (e) {
        console.error(e)
        error.value = e.message || 'No se pudo registrar la venta.'
    } finally {
        saving.value = false
    }
}
</script>
