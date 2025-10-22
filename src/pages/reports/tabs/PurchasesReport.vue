<script setup>
import { ref, computed } from 'vue'
import { getComprasAll, getProductoById } from '../api/usePurchasesApi'

const proveedorId = ref('')
const desde = ref('')
const hasta = ref('')
const loading = ref(false)
const error = ref('')

const compras = ref([])
const expanded = ref(new Set())

function toggle(id) {
    if (expanded.value.has(id)) expanded.value.delete(id)
    else expanded.value.add(id)
}

function fmtMoney(n) { return Number(n || 0).toFixed(0) }
function fmtDate(iso) {
    if (!iso) return ''
    try { return new Date(iso).toLocaleDateString() } catch { return iso }
}


// ...lo que ya tenés arriba
const page2 = ref(0)
const size2 = ref(10)

const totalPages2 = computed(() => {
    const total = compras.value.length
    const per = Math.max(1, Number(size2.value || 10))
    return Math.max(1, Math.ceil(total / per))
})

const pagedCompras = computed(() => {
    const start = page2.value * Math.max(1, Number(size2.value || 10))
    const end = start + Math.max(1, Number(size2.value || 10))
    return compras.value.slice(start, end)
})

function prev2() {
    if (page2.value > 0) {
        page2.value--
        expanded.value.clear() // colapsa al cambiar de página
    }
}
function next2() {
    if (page2.value + 1 < totalPages2.value) {
        page2.value++
        expanded.value.clear()
    }
}


const pageDaily = ref(0)
const sizeDaily = ref(10)

const totalPagesDaily = computed(() => {
    const total = comprasDiarias.value.length
    const per = Math.max(1, Number(sizeDaily.value || 10))
    return Math.max(1, Math.ceil(total / per))
})

const pagedComprasDiarias = computed(() => {
    const per = Math.max(1, Number(sizeDaily.value || 10))
    const start = pageDaily.value * per
    const end = start + per
    return comprasDiarias.value.slice(start, end)
})

function prevDaily() {
    if (pageDaily.value > 0) pageDaily.value--
}
function nextDaily() {
    if (pageDaily.value + 1 < totalPagesDaily.value) pageDaily.value++
}

// cuando recargás datos, vuelve a la página 0 y colapsa
async function load() {
    loading.value = true; error.value = ''; compras.value = []
    try {
        const list = await getComprasAll({
            proveedorId: proveedorId.value || undefined,
            desde: desde.value || undefined,
            hasta: hasta.value || undefined,
            size: 50,
        })
        await enrichProductNames(list)
        compras.value = list
        page2.value = 0
        pageDaily.value = 0
        expanded.value.clear()
    } catch (e) {
        error.value = String(e.message || e)
    } finally {
        loading.value = false
    }
}
load()

// ---------- Carga y enriquecimiento (solo FRONT) ----------
async function enrichProductNames(list) {
    const ids = new Set()
    for (const c of list) {
        const dets = c.items || c.detalles || []
        for (const it of dets) if (it?.productoId != null) ids.add(it.productoId)
    }
    const pairs = await Promise.allSettled(
        [...ids].map(id =>
            getProductoById(id).then(p => [id, (p.nombre || p.productoNombre || `#${id}`)])
        )
    )
    const nombres = new Map(pairs.filter(x => x.status === 'fulfilled').map(x => x.value))
    for (const c of list) {
        const dets = c.items || c.detalles || []
        for (const it of dets) {
            if (!it.productoNombre && it.productoId != null) {
                it.productoNombre = nombres.get(it.productoId) || `#${it.productoId}`
            }
        }
    }
}




const comprasDiarias = computed(() => {
    const map = new Map() // key = `${fecha}|${prov}`
    for (const c of compras.value) {
        const f = fmtDate(c.fechaEmision || c.fecha || c.creadoEn)
        const prov = c.proveedorId ?? c.proveedor?.id ?? '—'
        const k = `${f}|${prov}`
        if (!map.has(k)) map.set(k, { fecha: f, proveedorId: prov, cantCompras: 0, subtotal: 0, iva: 0, total: 0 })
        const row = map.get(k)
        row.cantCompras++
        row.subtotal += Number(c.subtotal || 0)
        row.iva += Number(c.iva || 0)
        row.total += Number(c.total || 0)
    }
    return [...map.values()].sort((a, b) => {
        if (a.fecha === b.fecha) return String(a.proveedorId).localeCompare(String(b.proveedorId))
        return new Date(b.fecha) - new Date(a.fecha)
    })
})
</script>

<template>
    <div>
        <h3>Reporte Compras</h3>

        <div class="filters">
            <div><label>Proveedor</label><input class="input" v-model="proveedorId" placeholder="ID opcional" /></div>
            <div><label>Desde</label><input class="input" type="date" v-model="desde" /></div>
            <div><label>Hasta</label><input class="input" type="date" v-model="hasta" /></div>
            <button class="button" @click="load" :disabled="loading">Consultar</button>
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
            <span style="margin-left:auto;">Total días: {{ comprasDiarias.length }}</span>
        </div>

        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Fecha</th>
                        <th>Proveedor</th>
                        <th># Compras</th>
                        <th>Subtotal</th>
                        <th>IVA</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
                    <!-- 👉 usa pagedComprasDiarias -->
                    <tr v-for="r in pagedComprasDiarias" :key="r.fecha + '|' + r.proveedorId">
                        <td>{{ r.fecha }}</td>
                        <td>{{ r.proveedorId }}</td>
                        <td>{{ r.cantCompras }}</td>
                        <td>{{ fmtMoney(r.subtotal) }}</td>
                        <td>{{ fmtMoney(r.iva) }}</td>
                        <td>{{ fmtMoney(r.total) }}</td>
                    </tr>
                    <tr v-if="!pagedComprasDiarias.length">
                        <td colspan="6" class="muted">Sin datos</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <!-- C) Listado de compras con sus ítems (evidencia directa de M2M) -->
        <h4 style="margin-top:1rem">Compras con sus ítems</h4>

        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th></th>
                        <th>Compra ID</th>
                        <th>Proveedor</th>
                        <th>Fecha</th>
                        <th>Subtotal</th>
                        <th>IVA</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
                    <!-- 👉 usa pagedCompras en vez de compras -->
                    <template v-for="c in pagedCompras" :key="c.id">
                        <tr>
                            <td>
                                <button class="button ghost" @click="toggle(c.id)">
                                    {{ expanded.has(c.id) ? '−' : '+' }}
                                </button>
                            </td>
                            <td>{{ c.id }}</td>
                            <td>{{ c.proveedorId ?? c.proveedor?.id ?? '—' }}</td>
                            <td>{{ fmtDate(c.fechaEmision || c.fecha || c.creadoEn) }}</td>
                            <td>{{ fmtMoney(c.subtotal) }}</td>
                            <td>{{ fmtMoney(c.iva) }}</td>
                            <td>{{ fmtMoney(c.total) }}</td>
                        </tr>
                        <tr v-if="expanded.has(c.id)">
                            <td></td>
                            <td colspan="6">
                                <table class="subtable">
                                    <thead>
                                        <tr>
                                            <th>Producto</th>
                                            <th>Cant.</th>
                                            <th>Costo Unit.</th>
                                            <th>Tasa IVA</th>
                                            <th>Subtotal línea</th>
                                            <th>Total línea</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="d in (c.items || c.detalles || [])"
                                            :key="(d.id ?? `${d.productoId}-${d.subtotalLinea}-${d.totalLinea}`)">
                                            <td>{{ d.productoNombre || `#${d.productoId}` }}</td>
                                            <td>{{ Number(d.cantidad || 0).toFixed(3) }}</td>
                                            <td>{{ fmtMoney(d.costoUnitario || 0) }}</td>
                                            <td>{{ d.tasaIva ?? 0 }}%</td>
                                            <td>{{ fmtMoney((d.subtotalLinea ?? d.subtotal ?? 0)) }}</td>
                                            <td>{{ fmtMoney((d.totalLinea ?? d.total ?? 0)) }}</td>
                                        </tr>
                                        <tr v-if="!((c.items || c.detalles || []).length)">
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
