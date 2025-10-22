<script setup>
import { ref, computed } from 'vue'
import { getVentasAll, getProductoById } from '../api/useSalesApi'

const clienteId = ref('')
const desde = ref('')
const hasta = ref('')
const loading = ref(false)
const error = ref('')

const ventas = ref([])           // [{ id, clienteId, fechaVenta, subtotal, iva, total, items:[] }]
const expanded = ref(new Set())  // ids expandidos

function toggle(id) {
    if (expanded.value.has(id)) expanded.value.delete(id)
    else expanded.value.add(id)
}

function fmtMoney(n) { return Number(n || 0).toFixed(0) }
function fmtDateTime(iso) {
    if (!iso) return ''
    try { return new Date(iso).toLocaleString() } catch { return iso }
}
function fmtDateOnly(iso) {
    if (!iso) return ''
    try { return new Date(iso).toLocaleDateString() } catch { return iso }
}

// ---------- Enriquecimiento: nombre de producto (front-only, sin caché persistente) ----------
async function enrichProductNames(list) {
    const ids = new Set()
    for (const v of list) {
        const dets = v.items || v.detalles || []
        for (const it of dets) if (it?.productoId != null) ids.add(it.productoId)
    }
    const pairs = await Promise.allSettled(
        [...ids].map(id => getProductoById(id).then(p => [id, (p.nombre || p.productoNombre || `#${id}`)]))
    )
    const nombres = new Map(pairs.filter(x => x.status === 'fulfilled').map(x => x.value))
    for (const v of list) {
        const dets = v.items || v.detalles || []
        for (const it of dets) {
            if (!it.productoNombre && it.productoId != null) {
                it.productoNombre = nombres.get(it.productoId) || `#${it.productoId}`
            }
        }
    }
}
// --- Paginación para "Ventas diarias por cliente"
const pageDaily = ref(0)
const sizeDaily = ref(10)

const totalPagesDaily = computed(() => {
    const total = ventasDiarias.value.length
    const per = Math.max(1, Number(sizeDaily.value || 10))
    return Math.max(1, Math.ceil(total / per))
})

const pagedVentasDiarias = computed(() => {
    const per = Math.max(1, Number(sizeDaily.value || 10))
    const start = pageDaily.value * per
    const end = start + per
    return ventasDiarias.value.slice(start, end)
})

function prevDaily() {
    if (pageDaily.value > 0) pageDaily.value--
}
function nextDaily() {
    if (pageDaily.value + 1 < totalPagesDaily.value) pageDaily.value++
}


// ---------- Carga ----------
async function fetchVentas() {
    loading.value = true; error.value = ''; ventas.value = []
    try {
        const list = await getVentasAll({
            clienteId: clienteId.value || undefined,
            desde: desde.value || undefined,
            hasta: hasta.value || undefined,
            size: 50,
        })
        await enrichProductNames(list)
        ventas.value = list
        // reset paginación/expansión
        page2.value = 0
        pageDaily.value = 0
        expanded.value.clear()
    } catch (e) {
        error.value = String(e.message || e)
    } finally {
        loading.value = false
    }
}
fetchVentas()

// ---------- Agregación: Ventas diarias por cliente (front-only) ----------
const ventasDiarias = computed(() => {
    const map = new Map() // key = `${fecha}|${cliente}`
    for (const v of ventas.value) {
        const f = fmtDateOnly(v.fechaVenta || v.fecha || v.creadoEn)
        const cli = v.clienteId ?? v.cliente?.id ?? '—'
        const k = `${f}|${cli}`
        if (!map.has(k)) map.set(k, { fecha: f, clienteId: cli, cantVentas: 0, subtotal: 0, iva: 0, total: 0 })
        const row = map.get(k)
        row.cantVentas++
        row.subtotal += Number(v.subtotal ?? 0)
        row.iva += Number(v.iva ?? 0)
        row.total += Number(v.total ?? 0)
    }
    // Orden fecha desc, cliente asc
    return [...map.values()].sort((a, b) => {
        if (a.fecha === b.fecha) return String(a.clienteId).localeCompare(String(b.clienteId))
        return new Date(b.fecha) - new Date(a.fecha)
    })
})

// ---------- Paginación local para la tabla de ventas con ítems ----------
const page2 = ref(0)
const size2 = ref(10)

const totalPages2 = computed(() => {
    const total = ventas.value.length
    const per = Math.max(1, Number(size2.value || 10))
    return Math.max(1, Math.ceil(total / per))
})

const pagedVentas = computed(() => {
    const per = Math.max(1, Number(size2.value || 10))
    const start = page2.value * per
    const end = start + per
    return ventas.value.slice(start, end)
})

function prev2() {
    if (page2.value > 0) {
        page2.value--
        expanded.value.clear()
    }
}
function next2() {
    if (page2.value + 1 < totalPages2.value) {
        page2.value++
        expanded.value.clear()
    }
}
</script>

<template>
    <div>
        <h3>Reporte Ventas</h3>

        <div class="filters">
            <div><label>Cliente</label><input class="input" v-model="clienteId" placeholder="ID opcional" /></div>
            <div><label>Desde</label><input class="input" type="datetime-local" v-model="desde" /></div>
            <div><label>Hasta</label><input class="input" type="datetime-local" v-model="hasta" /></div>
            <button class="button" @click="fetchVentas" :disabled="loading">Consultar</button>
            <span class="status" v-if="loading">Cargando…</span>
            <span class="status" v-else-if="error">Error: {{ error }}</span>
        </div>
        <!-- Controles de paginación (arriba) -->
        <div class="pagination-controls" style="display:flex;gap:.5rem;align-items:center;margin:.25rem 0;">
            <label style="display:flex;gap:.4rem;align-items:center;">
                Tamaño:
                <input class="input" type="number" min="1" v-model.number="sizeDaily" @change="pageDaily = 0"
                    style="width:6rem;" />
            </label>
            <button class="button ghost" @click="prevDaily" :disabled="pageDaily === 0">Anterior</button>
            <span>Página {{ pageDaily + 1 }} / {{ totalPagesDaily }}</span>
            <button class="button ghost" @click="nextDaily"
                :disabled="pageDaily + 1 >= totalPagesDaily">Siguiente</button>
            <span style="margin-left:auto;">Total días: {{ ventasDiarias.length }}</span>
        </div>

        <!-- A) Ventas diarias por cliente -->
        <h4 style="margin-top:1rem">A) Ventas diarias por cliente</h4>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Fecha</th>
                        <th>Cliente</th>
                        <th># Ventas</th>
                        <th>Subtotal</th>
                        <th>IVA</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
                    <!-- 👉 usa pagedVentasDiarias -->
                    <tr v-for="r in pagedVentasDiarias" :key="r.fecha + '|' + r.clienteId">
                        <td>{{ r.fecha }}</td>
                        <td>{{ r.clienteId }}</td>
                        <td>{{ r.cantVentas }}</td>
                        <td>{{ fmtMoney(r.subtotal) }}</td>
                        <td>{{ fmtMoney(r.iva) }}</td>
                        <td>{{ fmtMoney(r.total) }}</td>
                    </tr>
                    <tr v-if="!pagedVentasDiarias.length">
                        <td colspan="6" class="muted">Sin datos</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- C) Ventas con sus ítems (paginado local) -->
        <h4 style="margin-top:1rem">Ventas con sus ítems</h4>



        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th></th>
                        <th>Venta ID</th>
                        <th>Cliente</th>
                        <th>Fecha</th>
                        <th>Subtotal</th>
                        <th>IVA</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
                    <template v-for="v in pagedVentas" :key="v.id">
                        <tr>
                            <td>
                                <button class="button ghost" @click="toggle(v.id)">
                                    {{ expanded.has(v.id) ? '−' : '+' }}
                                </button>
                            </td>
                            <td>{{ v.id }}</td>
                            <td>{{ v.clienteId ?? v.cliente?.id ?? '—' }}</td>
                            <td>{{ fmtDateTime(v.fechaVenta || v.fecha || v.creadoEn) }}</td>
                            <td>{{ fmtMoney(v.subtotal) }}</td>
                            <td>{{ fmtMoney(v.iva) }}</td>
                            <td>{{ fmtMoney(v.total) }}</td>
                        </tr>
                        <tr v-if="expanded.has(v.id)">
                            <td></td>
                            <td colspan="6">
                                <table class="subtable">
                                    <thead>
                                        <tr>
                                            <th>Producto</th>
                                            <th>Cant.</th>
                                            <th>Precio Unit.</th>
                                            <th>Tasa IVA</th>
                                            <th>Subtotal línea</th>
                                            <th>Total línea</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="d in (v.items || v.detalles || [])"
                                            :key="(d.id ?? `${d.productoId}-${d.subtotalLinea}-${d.totalLinea}`)">
                                            <td>{{ d.productoNombre || `#${d.productoId}` }}</td>
                                            <td>{{ Number(d.cantidad ?? d.qty ?? 0).toFixed(3) }}</td>
                                            <td>{{ fmtMoney((d.precioUnitario ?? d.costoUnitario ?? 0)) }}</td>
                                            <td>{{ d.tasaIva ?? d.iva ?? 0 }}%</td>
                                            <td>{{ fmtMoney((d.subtotalLinea ?? d.subtotal ?? 0)) }}</td>
                                            <td>{{ fmtMoney((d.totalLinea ?? d.total ?? 0)) }}</td>
                                        </tr>
                                        <tr v-if="!((v.items || v.detalles || []).length)">
                                            <td colspan="6" class="muted">Sin ítems</td>
                                        </tr>
                                    </tbody>
                                </table>
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>

        <div class="pagination-controls" style="display:flex;gap:.5rem;align-items:center;margin-top:.5rem;">
            <button class="button ghost" @click="prev2" :disabled="page2 === 0">Anterior</button>
            <span>Página {{ page2 + 1 }} / {{ totalPages2 }}</span>
            <button class="button ghost" @click="next2" :disabled="page2 + 1 >= totalPages2">Siguiente</button>
        </div>
    </div>
</template>

<style scoped>
.filters {
    display: flex;
    gap: .5rem;
    align-items: end;
    flex-wrap: wrap;
    margin-bottom: .5rem;
}

.input {
    padding: .4rem .5rem;
}

.button {
    padding: .35rem .6rem;
    cursor: pointer;
}

.button.ghost {
    background: transparent;
    border: 1px solid #ddd;
}

.status {
    margin-left: .5rem;
}

.table-wrap {
    overflow: auto;
    margin-top: .25rem;
}

table {
    width: 100%;
    border-collapse: collapse;
}

th,
td {
    border: 1px solid #ddd;
    padding: .45rem .5rem;
}

.subtable {
    width: 100%;
    border-collapse: collapse;
    margin-top: .4rem;
}

.subtable th,
.subtable td {
    border: 1px solid #eee;
    padding: .35rem .45rem;
}

.muted {
    color: #777;
    font-style: italic;
    text-align: center;
}
</style>
