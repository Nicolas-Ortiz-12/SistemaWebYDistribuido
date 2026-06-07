<template>
    <section class="card">
        <div class="card-header">
            <div>
                <h2 class="title">Punto de Venta</h2>
                <p class="page-subtitle">Facturación rápida con lector de código de barras.</p>
            </div>
            <RouterLink class="btn btn-primary" to="/productos/lista">Ver catálogo</RouterLink>
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
                    <span>Lector de Código de Barras</span>
                    <input
                        v-model.trim="barcodeInput"
                        @keyup.enter="searchAndAddProduct"
                        placeholder="Escanea el código..."
                        autofocus
                        ref="barcodeInputRef"
                    />
                </label>
                <label class="input input-inline">
                    <span>Búsqueda manual en catálogo</span>
                    <input
                        v-model.trim="productQuery"
                        placeholder="Código, nombre o descripción"
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
                                    <!-- Fallback para productos escaneados no en la lista actual -->
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
                                <button type="button" class="btn btn-ghost" @click="addItem">+ Agregar ítem manual</button>
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
                    <span v-if="!saving">Cobrar (Pagado)</span>
                    <span v-else>Procesando...</span>
                </button>
                <button type="button" class="btn btn-secondary" @click="openDebtorModal" :disabled="saving || loading">
                    Anotar (Fiado)
                </button>
            </div>

            <div v-if="loading" class="loading">Cargando catálogo...</div>
            <div v-if="error" class="error">{{ error }}</div>
            <div v-if="ok" class="chip">Venta registrada correctamente</div>
        </div>

        <!-- Modal Fiado / Deudores -->
        <div v-if="showDebtorModal" class="modal-overlay" @click.self="showDebtorModal = false">
            <div class="modal-content">
                <h3>Venta a Crédito (Fiado)</h3>
                <p>Selecciona un deudor existente o crea uno nuevo para anotar esta venta por <strong>{{ money(total) }}</strong>.</p>
                
                <div class="form-group" style="margin-top: 1rem;">
                    <label>Deudores Existentes:</label>
                    <select v-model="selectedDebtorId" class="input">
                        <option :value="null">-- Crear Nuevo Deudor --</option>
                        <option v-for="d in debtors" :key="d.id" :value="d.id">
                            {{ d.nombre }} (Adeuda: {{ money(d.totalAdeudado) }})
                        </option>
                    </select>
                </div>

                <div v-if="selectedDebtorId === null" class="form-group" style="margin-top: 1rem;">
                    <label>Nombre del Nuevo Deudor:</label>
                    <input v-model.trim="newDebtorName" type="text" class="input" placeholder="Ej. Juan Pérez" />
                </div>

                <div class="modal-actions" style="margin-top: 2rem; display: flex; justify-content: flex-end; gap: 1rem;">
                    <button type="button" class="btn btn-ghost" @click="showDebtorModal = false">Cancelar</button>
                    <button type="button" class="btn btn-primary" @click="processTabSale" :disabled="saving">
                        Confirmar Fiado
                    </button>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup>
import { reactive, ref, computed, onMounted, watch, onUnmounted, nextTick } from "vue"
import { fetchWithAuth } from "../../services/authService"
import { API_BASE } from "../../services/api"

const today = new Date().toISOString().slice(0, 10)
const sale = reactive({
    date: today,
})

const products = ref([])
const productQuery = ref("")
const barcodeInput = ref("")
const barcodeInputRef = ref(null)

const debtors = ref([])
const showDebtorModal = ref(false)
const selectedDebtorId = ref(null)
const newDebtorName = ref("")

const loading = ref(false)
const saving = ref(false)
const error = ref("")
const ok = ref(false)

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

async function searchAndAddProduct() {
    const code = barcodeInput.value.trim()
    if (!code) return

    error.value = ""
    ok.value = false
    
    try {
        // Buscar en backend
        const res = await fetchWithAuth(`${API_BASE}/productos/search?barcode=${encodeURIComponent(code)}`)
        
        if (res && res.id) {
            // Verificar si ya está en la lista de items
            const existingItem = items.value.find(it => it.product_id === res.id)
            if (existingItem) {
                existingItem.qty += 1
            } else {
                // Agregar como nuevo item (limpiando filas vacías)
                if (items.value.length === 1 && items.value[0].product_id === 0) {
                    items.value = []
                }
                items.value.unshift({
                    product_id: res.id,
                    qty: 1,
                    unit_price: res.precio || 0,
                    tax_rate: res.tasaIva || 10,
                    nombre_temp: res.nombre
                })
            }
            barcodeInput.value = ""
        } else {
            error.value = "Producto no encontrado por código: " + code
            barcodeInput.value = ""
        }
    } catch (e) {
        error.value = "Producto no encontrado."
        barcodeInput.value = ""
    }
    
    // Devolver el foco al lector
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

async function fetchDebtors() {
    try {
        const data = await fetchWithAuth(`${API_BASE}/deudores/0/100`)
        debtors.value = Array.isArray(data?.content) ? data.content : []
    } catch (e) {
        console.error("Error cargando deudores", e)
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
        error.value = "Agrega al menos un ítem."
        return false
    }
    for (const it of items.value) {
        if (!it.product_id) {
            error.value = "Selecciona un producto en cada ítem."
            return false
        }
        if ((it.qty || 0) <= 0) {
            error.value = "La cantidad debe ser mayor a cero."
            return false
        }
        if ((it.tax_rate || 0) < 0) {
            error.value = "IVA inválido."
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
    fetchDebtors()
    selectedDebtorId.value = null
    newDebtorName.value = ""
    showDebtorModal.value = true
}

async function processTabSale() {
    let finalDebtorId = selectedDebtorId.value

    if (!finalDebtorId) {
        if (!newDebtorName.value.trim()) {
            alert("Debes seleccionar un deudor o ingresar un nombre nuevo.")
            return
        }
        saving.value = true
        try {
            const res = await fetchWithAuth(`${API_BASE}/deudores`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ nombre: newDebtorName.value.trim() })
            })
            finalDebtorId = res.id
        } catch (e) {
            saving.value = false
            alert("Error creando el nuevo deudor.")
            return
        }
    }

    showDebtorModal.value = false
    await confirmSale('FIADO', finalDebtorId)
}

async function confirmSale(estadoPago = 'PAGADO', deudorId = null) {
    if (!validate()) return
    ok.value = false
    error.value = ""
    saving.value = true

    try {
        const payload = {
            fechaVenta: new Date(sale.date).toISOString(),
            estadoPago: estadoPago,
            deudorId: deudorId,
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
})
onUnmounted(() => {
    if (productAborter) productAborter.abort()
    clearTimeout(productDebounce)
})
</script>

<style scoped>
.modal-overlay {
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}
.modal-content {
    background: var(--bg-card);
    padding: 2rem;
    border-radius: 12px;
    width: 90%;
    max-width: 400px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.15);
}
.modal-content h3 {
    margin-top: 0;
    margin-bottom: 0.5rem;
    font-size: 1.25rem;
}
</style>
