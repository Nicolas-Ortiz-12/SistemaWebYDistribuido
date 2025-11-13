<template>
    <section class="card">
        <div class="card-header">
            <h2 class="title">Ventas / Facturación</h2>
            <RouterLink class="btn" to="/productos/lista">Ver stock</RouterLink>
        </div>

        <div class="card-body">
            <div class="filters">
                <!-- Pestañas de modo -->
                <div class="tabs">
                    <button type="button" class="tab" :class="{ active: searchMode === 'ruc' }"
                        @click="switchMode('ruc')">
                        RUC
                    </button>
                    <button type="button" class="tab" :class="{ active: searchMode === 'nombre' }"
                        @click="switchMode('nombre')">
                        Nombre
                    </button>
                </div>

                <!-- Input + Botones -->
                <div class="searchbar">
                    <input :placeholder="searchMode === 'ruc' ? '80012345-6' : 'Juan Pérez / Supermercado XYZ'"
                        v-model.trim="sale.query" @keydown.enter.prevent="buscarCliente" />
                    <button class="btn btn-primary" type="button" @click="buscarCliente" :disabled="loadingCliente">
                        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <circle cx="11" cy="11" r="8"></circle>
                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        </svg>
                        <span v-if="!loadingCliente">Buscar</span>
                        <span v-else>Buscando…</span>
                    </button>
                    <button class="btn btn-ghost" type="button" @click="clearCliente" :disabled="loadingCliente">
                        Limpiar
                    </button>
                </div>

                <!-- Resultado elegido -->
                <div class="chosen">
                    <label class="input">
                        <span>Cliente</span>
                        <input :value="sale.cliente_nombre" placeholder="— sin cliente —" disabled />
                    </label>

                    <label class="input">
                        <span>Fecha</span>
                        <input type="date" v-model="sale.date" />
                    </label>
                </div>

                <!-- Lista de coincidencias (solo cuando hay varias al buscar por nombre) -->
                <div v-if="matches.length" class="matches">
                    <div class="matches-title">Coincidencias (click para seleccionar)</div>
                    <ul>
                        <li v-for="m in matches" :key="m.id" @click="elegirMatch(m)">
                            <div class="match-name">{{ m.nombre || m.razonSocial }}</div>
                            <div class="match-sub">{{ m.ruc || '—' }}</div>
                        </li>
                    </ul>
                </div>
            </div>

            <!-- ================= Detalle de venta ================= -->
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
                                <button class="btn eliminar" @click="removeItem(i)" title="Quitar">
                                    🗑️
                                </button>
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
            <div class="totales">
                <div>Subtotal: <strong>{{ money(subtotal) }}</strong></div>
                <div>IVA: <strong>{{ money(tax) }}</strong></div>
                <div>Total: <strong>{{ money(total) }}</strong></div>
            </div>

            <!-- Acciones -->
            <div class="acciones">
                <button class="btn btn-ghost" @click="saveDraft" :disabled="saving || loading">
                    Guardar borrador
                </button>
                <button class="btn btn-primary" @click="confirmSale" :disabled="saving || loading">
                    <span v-if="!saving">Confirmar venta</span>
                    <span v-else>Enviando…</span>
                </button>
            </div>

            <div v-if="loading" class="loading">Cargando catálogos…</div>
            <div v-if="error" class="error">{{ error }}</div>
            <div v-if="ok" class="chip">✅ Venta registrada correctamente</div>
        </div>
    </section>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from "vue"
import "../../assets/sale.css"
import { fetchWithAuth } from "../../services/authservice" // ⬅️ auth service

// Base de API unificada
const API_BASE = (
    import.meta.env.VITE_API_BASE ??
    import.meta.env.VITE_API_URL ??
    "http://localhost:8080"
).replace(/\/+$/, "")

/* ======================== Estado ======================== */
const today = new Date().toISOString().slice(0, 10)
const sale = reactive({
    customer_id: 0,
    cliente_nombre: "",
    query: "",
    date: today,
})

const searchMode = ref("ruc")
const matches = ref([])
const products = ref([])

const loading = ref(false)
const loadingCliente = ref(false)
const saving = ref(false)
const error = ref("")
const ok = ref(false)

/* ======================== Ítems ======================== */
const items = ref([{ product_id: 0, qty: 0, unit_price: 0, tax_rate: 10 }])
function addItem() {
    items.value.push({ product_id: 0, qty: 0, unit_price: 0, tax_rate: 10 })
}
function removeItem(i) {
    items.value.splice(i, 1)
}

function seedFromCatalog(i) {
    const it = items.value[i]
    const p = products.value.find((x) => x.id === it.product_id)
    if (p) {
        if (typeof p.precio === "number") it.unit_price = p.precio
        if (typeof p.tasaIva === "number") it.tax_rate = p.tasaIva
    }
}

/* ======================== Totales ======================== */
const lineTotal = (it) => (it.qty || 0) * (it.unit_price || 0)
const subtotal = computed(() =>
    items.value.reduce(
        (a, it) => a + (it.qty || 0) * (it.unit_price || 0),
        0
    )
)
const tax = computed(() =>
    items.value.reduce(
        (a, it) =>
            a +
            ((it.qty || 0) * (it.unit_price || 0) * (it.tax_rate || 0)) /
            100,
        0
    )
)
const total = computed(() => subtotal.value + tax.value)
const money = (v) =>
    new Intl.NumberFormat("es-PY", {
        style: "currency",
        currency: "PYG",
    }).format(v)

/* ======================== Cargar productos ======================== */
async function fetchProductos() {
    const data = await fetchWithAuth(`${API_BASE}/productos/0/200`)
    products.value = Array.isArray(data?.content)
        ? data.content
        : Array.isArray(data)
            ? data
            : []
}

onMounted(async () => {
    error.value = ""
    loading.value = true
    try {
        await fetchProductos()
    } catch (e) {
        console.error(e)
        error.value = e?.message || "No se pudieron cargar los productos."
    } finally {
        loading.value = false
    }
})

/* ======================== Búsqueda de cliente ======================== */
const normText = (s) => (s || "").toString().trim().replace(/\s+/g, " ")
const normRuc = (s) => normText(s).replace(/\s+/g, "").toUpperCase()

function switchMode(m) {
    searchMode.value = m
    matches.value = []
    sale.query = ""
    clearCliente()
}

function clearCliente() {
    sale.customer_id = 0
    sale.cliente_nombre = ""
    matches.value = []
}

function elegirMatch(cli) {
    sale.customer_id = Number(cli.id) || 0
    sale.cliente_nombre = cli.nombre || cli.razonSocial || ""
    if (cli.ruc) sale.query = normRuc(cli.ruc)
    matches.value = []
}

async function buscarCliente() {
    const q =
        searchMode.value === "ruc"
            ? normRuc(sale.query)
            : normText(sale.query)
    if (!q) {
        alert(
            `Ingresá un ${searchMode.value === "ruc" ? "RUC" : "nombre"
            } para buscar`
        )
        return
    }

    try {
        loadingCliente.value = true
        error.value = ""
        matches.value = []

        const data = await fetchWithAuth(
            `${API_BASE}/clientes/0/10/${encodeURIComponent(q)}`
        )

        const lista = Array.isArray(data?.content)
            ? data.content
            : Array.isArray(data)
                ? data
                : []

        if (!lista.length) {
            clearCliente()
            alert("No se encontraron clientes con ese criterio.")
            return
        }

        if (searchMode.value === "ruc") {
            const exact = lista.find((c) => normRuc(c.ruc) === q)
            elegirMatch(exact || lista[0])
        } else {
            if (lista.length === 1) {
                elegirMatch(lista[0])
            } else {
                const qLower = q.toLowerCase()
                matches.value = [...lista].sort((a, b) => {
                    const an = (a.nombre || a.razonSocial || "").toLowerCase()
                    const bn = (b.nombre || b.razonSocial || "").toLowerCase()
                    const aw = an.startsWith(qLower) ? 0 : 1
                    const bw = bn.startsWith(qLower) ? 0 : 1
                    return aw - bw || an.localeCompare(bn)
                })
            }
        }
    } catch (e) {
        console.error(e)
        error.value = e?.message || "No se pudo buscar el cliente."
    } finally {
        loadingCliente.value = false
    }
}


function validate() {
    if (!sale.date) {
        alert("Fecha obligatoria")
        return false
    }
    if (!sale.customer_id) {
        alert("Seleccioná un cliente (buscá por RUC o nombre)")
        return false
    }
    if (!items.value.length) {
        alert("Agrega al menos un ítem")
        return false
    }
    for (const it of items.value) {
        if (!it.product_id) {
            alert("Selecciona producto en todos los ítems")
            return false
        }
        if ((it.qty || 0) <= 0) {
            alert("Cantidad debe ser > 0")
            return false
        }
        if ((it.tax_rate || 0) < 0) {
            alert("IVA inválido")
            return false
        }
    }
    return true
}

function saveDraft() {
    if (!validate()) return
    ok.value = false
    error.value = ""
    alert(
        "Borrador guardado localmente. Si luego tenés endpoint de borradores, lo apunto ahí."
    )
}

async function confirmSale() {
    if (!validate()) return
    ok.value = false
    error.value = ""
    saving.value = true

    try {
        const payload = {
            clienteId: sale.customer_id,
            fechaVenta: new Date(sale.date).toISOString(),
            subtotal: Number(subtotal.value.toFixed(2)),
            iva: Number(tax.value.toFixed(2)),
            total: Number(total.value.toFixed(2)),
            items: items.value.map((it) => ({
                productoId: it.product_id,
                cantidad: it.qty,
                precioUnitario: it.unit_price,
                tasaIva: it.tax_rate,
            })),
        }

        await fetchWithAuth(`${API_BASE}/ventas`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        })

        ok.value = true
        clearCliente()
        sale.query = ""
        sale.date = new Date().toISOString().slice(0, 10)
        items.value = [{ product_id: 0, qty: 0, unit_price: 0, tax_rate: 10 }]
    } catch (e) {
        console.error(e)
        error.value = e?.message || "No se pudo registrar la venta."
    } finally {
        saving.value = false
    }
}
</script>
