<template>
    <section class="card">
        <div class="card-header">
            <h2 style="margin:0;font-size:18px">Compras / Facturación de compras</h2>
            <RouterLink class="btn" to="/existencias/stock">Ver stock</RouterLink>
        </div>

        <div class="card-body">
            <!-- Encabezado -->
            <div class="filters" style="margin-bottom:16px">
                <!-- Proveedor -->
                <label class="input">
                    <span>Proveedor</span>
                    <select v-model.number="compra.proveedorId" style="border:none;outline:none;width:100%">
                        <option :value="0" disabled>Selecciona proveedor</option>
                        <option v-for="p in proveedores" :key="p.id" :value="p.id">
                            {{ p.nombre }}
                        </option>
                    </select>
                </label>

                <!-- Número de comprobante -->
                <label class="input">
                    <span>Número</span>
                    <input v-model.trim="compra.numero" placeholder="000456" />
                </label>

                <!-- Fecha de emisión -->
                <label class="input">
                    <span>Fecha emisión</span>
                    <input type="date" v-model="compra.fechaEmision" />
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
                                <select v-model.number="it.productoId" style="width:100%" @change="seedFromCatalog(i)">
                                    <option :value="0" disabled>Selecciona producto</option>
                                    <option v-for="p in productos" :key="p.id" :value="p.id">
                                        {{ p.codigo || p.sku || ('#' + p.id) }} — {{ p.nombre }}
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
            <div style="display:flex;justify-content:flex-end;gap:18px;margin-top:12px;flex-wrap:wrap">
                <div>Subtotal: <strong>{{ money(subtotal) }}</strong></div>
                <div>IVA: <strong>{{ money(iva) }}</strong></div>
                <div>Total: <strong>{{ money(total) }}</strong></div>
            </div>

            <!-- Acciones -->
            <div style="display:flex;gap:10px;margin-top:16px">
                <button class="btn" :disabled="loading" @click="guardarBorrador">
                    Guardar borrador
                </button>
                <button class="btn" :disabled="loading" @click="confirmarCompra">
                    <span v-if="!loading">Confirmar compra</span>
                    <span v-else>Enviando…</span>
                </button>
            </div>

            <div v-if="error" class="error" style="margin-top:10px">{{ error }}</div>
            <div v-if="okMsg" class="chip" style="margin-top:10px">{{ okMsg }}</div>
        </div>
    </section>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from "vue"
import { fetchWithAuth } from "../../services/authservice" // ⬅️ auth service

// Base unificada con el resto de la app
const API_BASE = (
    import.meta.env.VITE_API_BASE ??
    import.meta.env.VITE_API_URL ??
    "http://localhost:8080"
).replace(/\/+$/, "")

const today = new Date().toISOString().slice(0, 10)
const compra = reactive({
    proveedorId: 0,
    numero: "",
    fechaEmision: today,
})

const items = ref([{ productoId: 0, cantidad: 0, costoUnitario: 0, tasaIva: 10 }])

const proveedores = ref([])
const productos = ref([])

const loading = ref(false)
const error = ref("")
const okMsg = ref("")

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
    const data = await fetchWithAuth(`${API_BASE}/productos/0/50`)
    productos.value = Array.isArray(data?.content)
        ? data.content
        : Array.isArray(data)
            ? data
            : []
}

/** ---------- API: POST /compras ---------- **/
async function confirmarCompra() {
    if (!validate()) return
    loading.value = true
    error.value = ""
    okMsg.value = ""

    try {
        const payload = {
            proveedorId: compra.proveedorId,
            numero: compra.numero.trim(),
            fechaEmision: new Date(compra.fechaEmision).toISOString(),
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
        // Reset suave
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
</script>
