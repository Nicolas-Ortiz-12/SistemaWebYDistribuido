<template>
    <section class="card">
        <div class="card-header">
            <h2 style="margin:0;font-size:18px">Compras / Facturación de compras</h2>
            <RouterLink class="btn btn-primary" to="/productos/lista">Ver catálogo</RouterLink>
        </div>

        <div class="card-body">
            <div class="filters" style="margin: 0 0 12px 0">
                <label class="input" style="flex:1 1 320px">
                    <span>Buscar producto en catálogo</span>
                    <div style="display: flex; gap: 8px;">
                        <input
                            v-model.trim="productQuery"
                            placeholder="Código, nombre o descripción"
                            style="flex: 1;"
                        />
                        <button type="button" class="btn btn-secondary" @click="showScanner = true" style="display: flex; align-items: center; justify-content: center; width: 42px; height: 42px; border-radius: var(--radius-md); background: var(--bg-card); border: 1px solid var(--border-color); color: var(--text-color); cursor: pointer;" title="Escanear con cámara">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
                              <path stroke-linecap="round" stroke-linejoin="round" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                              <circle cx="12" cy="13" r="4" />
                            </svg>
                        </button>
                    </div>
                </label>
            </div>

            <div class="filters" style="margin-bottom:16px">
                <label class="input">
                    <span>Proveedor</span>
                    <select v-model.number="compra.proveedorId">
                        <option :value="0" disabled>Selecciona proveedor</option>
                        <option v-for="p in proveedores" :key="p.id" :value="p.id">
                            {{ p.nombre }}
                        </option>
                    </select>
                </label>

                <label class="input">
                    <span>Número</span>
                    <input v-model.trim="compra.numero" placeholder="000456" />
                </label>

                <label class="input">
                    <span>Fecha emisión</span>
                    <input type="date" v-model="compra.fechaEmision" />
                </label>
            </div>

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
                                <select v-model.number="it.productoId" style="width:100%" @change="seedFromCatalog(i)">
                                    <option :value="0" disabled>Selecciona producto</option>
                                    <option v-for="p in productos" :key="p.id" :value="p.id">
                                        {{ p.codigo || p.sku || ('#' + p.id) }} - {{ p.nombre }}
                                    </option>
                                </select>
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.001" v-model.number="it.cantidad"
                                    style="width:100%" />
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.01" v-model.number="it.costoUnitario"
                                    style="width:100%" />
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.01" v-model.number="it.tasaIva"
                                    style="width:100%" />
                            </td>
                            <td class="td-right">{{ money(lineTotal(it)) }}</td>
                            <td>
                                <button type="button" class="mini-btn danger" @click="removeItem(i)" title="Quitar">
                                    x
                                </button>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="6" style="padding:10px">
                                <button type="button" class="btn btn-ghost" @click="addItem">+ Agregar item</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div style="display:flex;justify-content:flex-end;gap:18px;margin-top:12px;flex-wrap:wrap">
                <div>Subtotal: <strong>{{ money(subtotal) }}</strong></div>
                <div>IVA: <strong>{{ money(iva) }}</strong></div>
                <div>Total: <strong>{{ money(total) }}</strong></div>
            </div>

            <div style="display:flex;gap:10px;margin-top:16px">
                <button type="button" class="btn btn-ghost" :disabled="loading" @click="guardarBorrador">
                    Guardar borrador
                </button>
                <button type="button" class="btn btn-primary" :disabled="loading" @click="confirmarCompra">
                    <span v-if="!loading">Confirmar compra</span>
                    <span v-else>Enviando...</span>
                </button>
            </div>

            <div v-if="error" class="error" style="margin-top:10px">{{ error }}</div>
            <div v-if="okMsg" class="chip" style="margin-top:10px">{{ okMsg }}</div>
        </div>
        <CameraScanner :show="showScanner" @close="showScanner = false" @scan="searchAndAddProduct" />
    </section>
</template>

<script setup>
import { reactive, ref, computed, onMounted, watch, onUnmounted } from "vue"
import { fetchWithAuth } from "../../services/authService"
import { API_BASE } from "../../services/api"
import CameraScanner from "../../components/CameraScanner.vue"

const showScanner = ref(false)

const today = new Date().toISOString().slice(0, 10)
const compra = reactive({
    proveedorId: 0,
    numero: "",
    fechaEmision: today,
})

const items = ref([{ productoId: 0, cantidad: 0, costoUnitario: 0, tasaIva: 10 }])

const proveedores = ref([])
const productos = ref([])
const productQuery = ref("")

const loading = ref(false)
const error = ref("")
const okMsg = ref("")

let productAborter = null
let productDebounce = null

const money = (v) =>
    new Intl.NumberFormat("es-PY", {
        style: "currency",
        currency: "PYG",
    }).format(Number(v) || 0)

const lineTotal = (it) => (it.cantidad || 0) * (it.costoUnitario || 0)

const subtotal = computed(() =>
    items.value.reduce(
        (a, it) => a + (it.cantidad || 0) * (it.costoUnitario || 0),
        0
    )
)

const iva = computed(() =>
    items.value.reduce((a, it) => {
        const base = (it.cantidad || 0) * (it.costoUnitario || 0)
        return a + base * ((it.tasaIva || 0) / 100)
    }, 0)
)

const total = computed(() => subtotal.value + iva.value)

function addItem() {
    items.value.push({ productoId: 0, cantidad: 0, costoUnitario: 0, tasaIva: 10 })
}
function removeItem(i) {
    items.value.splice(i, 1)
}

function seedFromCatalog(i) {
    const it = items.value[i]
    const p = productos.value.find((x) => x.id === it.productoId)
    if (p) {
        const costo = Number.isFinite(p.costo)
            ? p.costo
            : Number.isFinite(p.precio)
                ? p.precio
                : 0
        it.costoUnitario = costo
        if (Number.isFinite(p.tasaIva)) it.tasaIva = p.tasaIva
    }
}

function validate() {
    error.value = ""
    okMsg.value = ""
    if (!compra.proveedorId) {
        error.value = "Selecciona un proveedor."
        return false
    }
    if (!compra.numero?.trim()) {
        error.value = "Número de comprobante es obligatorio."
        return false
    }
    if (!compra.fechaEmision) {
        error.value = "Fecha de emisión es obligatoria."
        return false
    }
    if (!items.value.length) {
        error.value = "Agrega al menos un ítem."
        return false
    }
    for (const it of items.value) {
        if (!it.productoId) {
            error.value = "Selecciona el producto en todos los ítems."
            return false
        }
        if ((it.cantidad || 0) <= 0) {
            error.value = "Cantidad debe ser > 0."
            return false
        }
        if ((it.costoUnitario || 0) < 0) {
            error.value = "Costo unitario no puede ser negativo."
            return false
        }
        if (!Number.isFinite(it.tasaIva) || it.tasaIva < 0 || it.tasaIva > 100) {
            error.value = "La tasa de IVA debe estar entre 0 y 100."
            return false
        }
    }
    return true
}

async function fetchProveedores() {
    const data = await fetchWithAuth(`${API_BASE}/proveedores/0/50`)
    proveedores.value = Array.isArray(data?.content)
        ? data.content
        : Array.isArray(data)
            ? data
            : []
}

async function fetchProductos() {
    if (productAborter) productAborter.abort()
    productAborter = new AbortController()
    const q = productQuery.value?.trim()
    const endpoint = q
        ? `${API_BASE}/productos/0/50/${encodeURIComponent(q)}`
        : `${API_BASE}/productos/0/50`

    const data = await fetchWithAuth(endpoint, {
        signal: productAborter.signal,
    })
    productos.value = Array.isArray(data?.content)
        ? data.content
        : Array.isArray(data)
            ? data
            : []
}

async function searchAndAddProduct(code) {
    if (!code) return
    error.value = ""
    okMsg.value = ""

    try {
        const res = await fetchWithAuth(`${API_BASE}/productos/search?barcode=${encodeURIComponent(code)}`)
        if (res && res.id) {
            // Add product to local array if not present to ensure <select> works
            if (!productos.value.find(p => p.id === res.id)) {
                productos.value.unshift(res)
            }
            
            const existingItem = items.value.find((it) => it.productoId === res.id)
            if (existingItem) {
                existingItem.cantidad += 1
            } else {
                if (items.value.length === 1 && items.value[0].productoId === 0) {
                    items.value = []
                }
                const costo = Number.isFinite(res.costo) ? res.costo : (Number.isFinite(res.precio) ? res.precio : 0)
                items.value.unshift({
                    productoId: res.id,
                    cantidad: 1,
                    costoUnitario: costo,
                    tasaIva: res.tasaIva || 10,
                })
            }
        } else {
            error.value = "Producto no encontrado por código: " + code
        }
    } catch (e) {
        console.error(e)
        error.value = "Error buscando producto por código de barras."
    }
}

async function confirmarCompra() {
    if (!validate()) return
    loading.value = true
    error.value = ""
    okMsg.value = ""

    try {
        const payload = {
            proveedorId: compra.proveedorId,
            numero: compra.numero.trim(),
            fechaEmision: new Date(compra.fechaEmision + 'T00:00:00').toISOString(),
            detalles: items.value.map((it) => ({
                productoId: it.productoId,
                cantidad: it.cantidad,
                costoUnitario: it.costoUnitario,
                tasaIva: it.tasaIva,
            })),
            subtotal: Number(subtotal.value.toFixed(2)),
            iva: Number(iva.value.toFixed(2)),
            total: Number(total.value.toFixed(2)),
        }

        await fetchWithAuth(`${API_BASE}/compras`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        })

        okMsg.value = "Compra registrada correctamente."
        items.value = [{ productoId: 0, cantidad: 0, costoUnitario: 0, tasaIva: 10 }]
        compra.numero = ""
    } catch (e) {
        error.value = e?.message || "No se pudo registrar la compra."
    } finally {
        loading.value = false
    }
}

function guardarBorrador() {
    if (!validate()) return
    localStorage.setItem(
        "draft_compra",
        JSON.stringify({ compra: { ...compra }, items: items.value })
    )
    okMsg.value = "Borrador guardado en este equipo."
}

onMounted(async () => {
    try {
        await Promise.all([fetchProveedores(), fetchProductos()])
    } catch (e) {
        error.value = e?.message || "Error cargando catálogos."
    }
})

watch(productQuery, () => {
    clearTimeout(productDebounce)
    productDebounce = setTimeout(() => {
        fetchProductos().catch((e) => {
            if (e?.name !== "AbortError") {
                console.error(e)
                error.value = e?.message || "No se pudieron filtrar los productos."
            }
        })
    }, 300)
})

onUnmounted(() => {
    if (productAborter) productAborter.abort()
    clearTimeout(productDebounce)
})
</script>
