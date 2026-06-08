<template>
    <section class="card">
        <div class="card-header">
            <div>
                <h2 class="title">Punto de Venta</h2>
                <p class="page-subtitle">Facturacion rapida con lector de codigo de barras.</p>
            </div>
            <RouterLink class="btn btn-primary" to="/productos/lista">Ver catalogo</RouterLink>
        </div>

        <div class="card-body">
            <div class="filters">
                <label class="input">
                    <span>Fecha</span>
                    <input type="date" v-model="sale.date" />
                </label>

                <div class="chip sale-chip">Caja Activa</div>
            </div>

            <div class="filters" style="margin-bottom:12px; display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                <label class="input input-inline">
                    <span>Lector de Codigo de Barras</span>
                    <div style="display: flex; gap: 8px;">
                        <input
                            v-model.trim="barcodeInput"
                            @keyup.enter="searchAndAddProduct(barcodeInput)"
                            placeholder="Escanea el codigo..."
                            ref="barcodeInputRef"
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
                <label class="input input-inline">
                    <span>Busqueda manual en catalogo</span>
                    <input
                        v-model.trim="productQuery"
                        placeholder="Codigo, nombre o descripcion"
                    />
                </label>
            </div>

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
                                        {{ p.codigoBarras || p.codigo || p.id }} - {{ p.nombre }}
                                    </option>
                                    <option v-if="it.product_id && !products.find(x => x.id === it.product_id)" :value="it.product_id">
                                        {{ it.nombre_temp }}
                                    </option>
                                </select>
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.001" v-model.number="it.qty" style="width:100%" />
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.01" v-model.number="it.unit_price" style="width:100%" />
                            </td>
                            <td class="td-right">
                                <input type="number" min="0" step="0.01" v-model.number="it.tax_rate" style="width:100%" />
                            </td>
                            <td class="td-right">{{ money(lineTotal(it)) }}</td>
                            <td>
                                <button type="button" class="mini-btn danger" @click="removeItem(i)" title="Quitar">
                                    Quitar
                                </button>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="6" style="padding:10px">
                                <button type="button" class="btn btn-ghost" @click="addItem">+ Agregar item manual</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="totales">
                <div>Subtotal: <strong>{{ money(subtotal) }}</strong></div>
                <div>IVA: <strong>{{ money(tax) }}</strong></div>
                <div>Total: <strong>{{ money(total) }}</strong></div>
            </div>

            <div class="acciones">
                <button type="button" class="btn btn-ghost" @click="saveDraft" :disabled="saving || loading">
                    Guardar borrador
                </button>
                <button type="button" class="btn btn-primary" @click="confirmSale('PAGADO')" :disabled="saving || loading">
                    <span v-if="!saving">Charge (Cash)</span>
                    <span v-else>Procesando...</span>
                </button>
                <button type="button" class="btn btn-secondary" @click="openDebtorModal" :disabled="saving || loading">
                    Put on Tab
                </button>
            </div>

            <div v-if="loading" class="loading">Cargando catalogo...</div>
            <div v-if="error" class="error">{{ error }}</div>
            <div v-if="ok" class="chip">Venta registrada correctamente</div>
        </div>

        <div v-if="showDebtorModal" class="modal-overlay" @click.self="closeDebtorModal">
            <div class="debtor-modal" role="dialog" aria-modal="true" aria-labelledby="debtor-modal-title">
                <div class="debtor-modal-header">
                    <div>
                        <p class="debtor-modal-kicker">Tab sale</p>
                        <h3 id="debtor-modal-title">Put on Tab</h3>
                    </div>
                    <strong class="debtor-modal-total">{{ money(total) }}</strong>
                </div>
                <p class="debtor-modal-copy">Enter the person's name and the backend will find or create the debtor.</p>

                <label class="debtor-field">
                    <span>Person's Name</span>
                    <input
                        v-model.trim="newDebtorName"
                        type="text"
                        class="debtor-control"
                        placeholder="Juan Perez"
                        ref="debtorNameInputRef"
                    />
                </label>

                <div class="modal-actions">
                    <button type="button" class="btn btn-ghost" @click="closeDebtorModal">Cancelar</button>
                    <button type="button" class="btn btn-primary" @click="processTabSale" :disabled="saving">
                        <span v-if="!saving">Confirm Tab</span>
                        <span v-else>Procesando...</span>
                    </button>
                </div>
            </div>
        </div>
        <CameraScanner :show="showScanner" @close="showScanner = false" @scan="searchAndAddProduct" />
    </section>
</template>

<script setup>
import { reactive, ref, shallowRef, computed, onMounted, watch, onUnmounted, nextTick } from "vue"
import { fetchWithAuth } from "../../services/authService"
import { API_BASE } from "../../services/api"
import CameraScanner from "../../components/CameraScanner.vue"

const showScanner = ref(false)

const today = new Date().toISOString().slice(0, 10)
const sale = reactive({
    date: today,
})

const products = ref([])
const productQuery = shallowRef("")
const barcodeInput = shallowRef("")
const barcodeInputRef = ref(null)
const debtorNameInputRef = ref(null)

const showDebtorModal = shallowRef(false)
const newDebtorName = shallowRef("")

const loading = shallowRef(false)
const saving = shallowRef(false)
const error = shallowRef("")
const ok = shallowRef(false)

let productAborter = null
let productDebounce = null

const items = ref([])

function addItem() {
    items.value.push({ product_id: 0, qty: 1, unit_price: 0, tax_rate: 10, nombre_temp: "" })
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
        it.nombre_temp = p.nombre
    }
}

async function searchAndAddProduct(scannedCode = barcodeInput.value) {
    const code = String(scannedCode ?? "").trim()
    if (!code) return

    error.value = ""
    ok.value = false

    try {
        const res = await fetchWithAuth(`${API_BASE}/productos/search?barcode=${encodeURIComponent(code)}`)

        if (res && res.id) {
            const existingItem = items.value.find((it) => it.product_id === res.id)
            if (existingItem) {
                existingItem.qty += 1
            } else {
                if (items.value.length === 1 && items.value[0].product_id === 0) {
                    items.value = []
                }
                items.value.unshift({
                    product_id: res.id,
                    qty: 1,
                    unit_price: res.precio || 0,
                    tax_rate: res.tasaIva || 10,
                    nombre_temp: res.nombre,
                })
            }
            barcodeInput.value = ""
        } else {
            error.value = "Producto no encontrado por codigo: " + code
            barcodeInput.value = ""
        }
    } catch (e) {
        error.value = e?.message || "Producto no encontrado."
        barcodeInput.value = ""
    }

    nextTick(() => {
        barcodeInputRef.value?.focus()
    })
}

const lineTotal = (it) => (it.qty || 0) * (it.unit_price || 0)
const subtotal = computed(() =>
    items.value.reduce((a, it) => a + (it.qty || 0) * (it.unit_price || 0), 0)
)
const tax = computed(() =>
    items.value.reduce(
        (a, it) => a + ((it.qty || 0) * (it.unit_price || 0) * (it.tax_rate || 0)) / 100,
        0
    )
)
const total = computed(() => subtotal.value + tax.value)
const money = (v) =>
    new Intl.NumberFormat("es-PY", {
        style: "currency",
        currency: "PYG",
    }).format(v)

async function fetchProductos() {
    if (productAborter) productAborter.abort()
    productAborter = new AbortController()
    loading.value = true
    error.value = ""

    try {
        const q = productQuery.value?.trim()
        const endpoint = q
            ? `${API_BASE}/productos/0/50/${encodeURIComponent(q)}`
            : `${API_BASE}/productos/0/50`

        const data = await fetchWithAuth(endpoint, { signal: productAborter.signal })
        products.value = Array.isArray(data?.content)
            ? data.content
            : Array.isArray(data)
                ? data
                : []
    } catch (e) {
        if (e?.name !== "AbortError") {
            console.error(e)
            error.value = e?.message || "No se pudieron cargar los productos."
        }
    } finally {
        loading.value = false
    }
}

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

function validate() {
    error.value = ""
    if (!sale.date) {
        error.value = "Selecciona la fecha."
        return false
    }
    if (!items.value.length) {
        error.value = "Agrega al menos un item."
        return false
    }
    for (const it of items.value) {
        if (!it.product_id) {
            error.value = "Selecciona un producto en cada item."
            return false
        }
        if ((it.qty || 0) <= 0) {
            error.value = "La cantidad debe ser mayor a cero."
            return false
        }
        if ((it.unit_price || 0) < 0 || it.unit_price == null) {
            error.value = "Precio unitario invalido."
            return false
        }
        if ((it.tax_rate || 0) < 0) {
            error.value = "IVA invalido."
            return false
        }
    }
    return true
}

function saveDraft() {
    if (!validate()) return
    ok.value = false
    localStorage.setItem(
        "draft_venta_mostrador",
        JSON.stringify({ sale: { ...sale }, items: items.value })
    )
    ok.value = true
}

function openDebtorModal() {
    if (!validate()) return
    error.value = ""
    newDebtorName.value = ""
    showDebtorModal.value = true
    nextTick(() => {
        debtorNameInputRef.value?.focus()
    })
}

function closeDebtorModal() {
    if (saving.value) return
    showDebtorModal.value = false
}

async function processTabSale() {
    const debtorName = newDebtorName.value.trim()
    if (!debtorName) {
        alert("Debes ingresar el nombre de la persona.")
        return
    }

    showDebtorModal.value = false
    await confirmSale("FIADO", debtorName)
}

async function confirmSale(estadoPago = "PAGADO", deudorNombre = null) {
    if (!validate()) return
    ok.value = false
    error.value = ""
    saving.value = true

    try {
        const payload = {
            fechaVenta: new Date(sale.date + "T00:00:00").toISOString(),
            estadoPago,
            deudorNombre: estadoPago === "FIADO" ? (deudorNombre?.trim() || null) : null,
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
        sale.date = new Date().toISOString().slice(0, 10)
        items.value = []
        setTimeout(() => {
            barcodeInputRef.value?.focus()
        }, 100)
    } catch (e) {
        console.error(e)
        error.value = e?.message || "No se pudo registrar la venta."
    } finally {
        saving.value = false
    }
}

onMounted(() => {
    fetchProductos()
    if (items.value.length === 0) {
        addItem()
    }
    nextTick(() => {
        barcodeInputRef.value?.focus()
    })
})

onUnmounted(() => {
    if (productAborter) productAborter.abort()
    clearTimeout(productDebounce)
})
</script>

<style scoped>
.modal-overlay {
    position: fixed;
    inset: 0;
    padding: 20px;
    background: rgba(20, 18, 14, 0.58);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}

.debtor-modal {
    width: min(520px, 100%);
    max-height: calc(100vh - 40px);
    overflow: auto;
    background: var(--panel, #fff);
    border: 1px solid var(--border, #ddd7c8);
    border-radius: 18px;
    padding: 22px;
    box-shadow: 0 24px 64px rgba(24, 20, 12, 0.28);
}

.debtor-modal-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 8px;
}

.debtor-modal-kicker {
    margin: 0 0 4px;
    color: var(--muted, #6d6a62);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.debtor-modal h3 {
    margin: 0;
    font-size: 22px;
    line-height: 1.15;
}

.debtor-modal-total {
    flex: 0 0 auto;
    padding: 8px 10px;
    border-radius: 12px;
    background: #fff7dc;
    color: var(--primary-text, #2b2108);
    border: 1px solid rgba(220, 167, 45, 0.35);
}

.debtor-modal-copy {
    margin: 0 0 18px;
    color: var(--muted, #6d6a62);
    line-height: 1.45;
}

.debtor-field {
    display: grid;
    gap: 7px;
    margin-top: 14px;
    color: var(--muted, #6d6a62);
    font-size: 13px;
    font-weight: 600;
}

.debtor-control {
    width: 100%;
    min-height: 48px;
    border: 1px solid var(--border, #ddd7c8);
    border-radius: 12px;
    padding: 0 12px;
    background: #fff;
    color: var(--text, #1f2328);
}

.modal-actions {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    flex-wrap: wrap;
}

.btn-secondary {
    background: linear-gradient(180deg, #f5e3a5 0%, #e2b83f 100%);
    border-color: #c49b24;
    color: var(--primary-text, #2b2108);
    box-shadow: 0 12px 24px rgba(194, 151, 35, 0.18);
}

@media (max-width: 640px) {
    .modal-overlay {
        align-items: flex-end;
        padding: 12px;
    }

    .debtor-modal {
        border-radius: 16px;
        padding: 18px;
    }

    .debtor-modal-header {
        display: grid;
    }

    .debtor-modal-total {
        justify-self: start;
    }
}
</style>
