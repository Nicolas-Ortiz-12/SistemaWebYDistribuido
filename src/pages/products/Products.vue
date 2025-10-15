<script setup>
import { ref, watch, onMounted, onUnmounted } from "vue"
import "../../assets/productos.css"
import ProductoForm from "../../components/ProductoForm.vue"

const API_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/, "")

// Estados
const busqueda = ref("")
const productos = ref([])
const page = ref(0) // 0-based
const size = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)
const loading = ref(false)
const error = ref("")

let aborter = null
let debounceId = null

// Formateo precios
const fmt = new Intl.NumberFormat("es-PY", { style: "currency", currency: "PYG", maximumFractionDigits: 0 })
const fmtPrecio = v => (isFinite(v) ? fmt.format(v) : "—")

// Normalizador
function normalizeProducto(p = {}) {
    return {
        id: p.id ?? null,
        codigo: p.codigo ?? "",
        nombre: p.nombre ?? "",
        categoriaId: p.categoriaId ?? null,
        costo: p.costo ?? 0,
        precio: p.precio ?? 0,
        stockMinimo: p.stockMinimo ?? 0,
        unidadMedida: p.unidadMedida ?? "UN",
        descripcion: p.descripcion ?? ""
    }
}

// ========= CARGA DE PRODUCTOS =========
async function fetchProductos() {
    if (aborter) aborter.abort()
    aborter = new AbortController()
    loading.value = true
    error.value = ""

    try {
        const p = page.value
        const s = size.value
        const q = busqueda.value?.trim()

        // tu API: /productos/{page}/{size}/{q} (opcional q)
        let endpoint = `${API_URL}/productos/${p}/${s}`
        if (q) endpoint += `/${encodeURIComponent(q)}`

        const resp = await fetch(endpoint, { signal: aborter.signal })
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
        const data = await resp.json()

        const content = Array.isArray(data?.content)
            ? data.content
            : Array.isArray(data)
                ? data
                : []
        productos.value = content.map(normalizeProducto)
        totalPages.value = data.totalPages ?? 1
        totalElements.value = data.totalElements ?? productos.value.length
    } catch (e) {
        if (e.name !== "AbortError") {
            console.error(e)
            error.value = "No se pudieron cargar los productos."
        }
    } finally {
        loading.value = false
    }
}

// ========= PAGINACIÓN =========
function siguiente() {
    if (page.value < totalPages.value - 1) page.value++
}
function anterior() {
    if (page.value > 0) page.value--
}
function primero() {
    page.value = 0
}
function ultimo() {
    page.value = totalPages.value - 1
}

// ========= WATCHERS =========
watch(busqueda, () => {
    page.value = 0
    clearTimeout(debounceId)
    debounceId = setTimeout(fetchProductos, 400)
})
watch([page, size], fetchProductos)

onMounted(fetchProductos)
onUnmounted(() => {
    if (aborter) aborter.abort()
    clearTimeout(debounceId)
})

// ========= CRUD =========
async function editarProducto(p) {
    const nombre = prompt("Editar nombre:", p.nombre)
    if (!nombre?.trim()) return
    const descripcion = prompt("Editar descripción:", p.descripcion)
    try {
        loading.value = true
        const resp = await fetch(`${API_URL}/productos/${p.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ ...p, nombre: nombre.trim(), descripcion: descripcion ?? "" })
        })
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
        await fetchProductos()
    } catch (e) {
        console.error(e)
        error.value = "No se pudo actualizar el producto."
    } finally {
        loading.value = false
    }
}

async function eliminarProducto(p) {
    if (!confirm(`¿Eliminar producto "${p.nombre}"?`)) return
    try {
        loading.value = true
        const resp = await fetch(`${API_URL}/productos/${p.id}`, { method: "DELETE" })
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
        if (productos.value.length === 1 && page.value > 0) {
            page.value--
        } else {
            await fetchProductos()
        }
    } catch (e) {
        console.error(e)
        error.value = "No se pudo eliminar el producto."
    } finally {
        loading.value = false
    }
}

// ========= MODAL NUEVO =========
const showCreate = ref(false)
function nuevoProducto() { showCreate.value = true }
function onCloseForm() { showCreate.value = false }
function onSavedProducto() {
    showCreate.value = false
    fetchProductos()
}
</script>

<template>
    <section class="card productos">
        <div class="card-header">
            <h2 style="margin:0">Lista de productos</h2>
            <button class="btn nuevo" @click="nuevoProducto">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" width="18" height="18">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                Nuevo producto
            </button>
        </div>

        <div class="card-body">
            <div class="filters">
                <label class="input" style="grid-column: 1 / -1">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" width="18" height="18">
                        <circle cx="11" cy="11" r="8" />
                        <line x1="21" y1="21" x2="16.65" y2="16.65" />
                    </svg>
                    <input type="text" placeholder="Buscar por nombre o descripción" v-model="busqueda"
                        class="buscar" />
                </label>

                <label class="input" style="max-width:150px">
                    <span style="font-size:12px;color:#6b7280">Items por página</span>
                    <select v-model.number="size" :disabled="loading"
                        style="width:100%;border:none;background:transparent">
                        <option :value="10">10</option>
                        <option :value="20">20</option>
                        <option :value="50">50</option>
                    </select>
                </label>
            </div>

            <div v-if="error" class="error">{{ error }}</div>
            <div v-if="loading" class="loading">Cargando…</div>

            <div v-if="!loading" class="tabla-container">
                <table>
                    <thead>
                        <tr>
                            <th style="width:80px">ID</th>
                            <th>Nombre</th>
                            <th>Descripción</th>
                            <th style="width:140px">Precio</th>
                            <th style="width:100px">Unidad</th>
                            <th style="width:90px">Editar</th>
                            <th style="width:100px">Eliminar</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="p in productos" :key="p.id">
                            <td>{{ p.id }}</td>
                            <td>{{ p.nombre }}</td>
                            <td>{{ p.descripcion }}</td>
                            <td>{{ fmtPrecio(p.precio) }}</td>
                            <td>{{ p.unidadMedida }}</td>
                            <td><button class="btn editar" @click="editarProducto(p)">✏️</button></td>
                            <td><button class="btn eliminar" @click="eliminarProducto(p)">🗑️</button></td>
                        </tr>
                        <tr v-if="productos.length === 0">
                            <td colspan="7" style="text-align:center; color:#6b7280; padding:12px">
                                Sin resultados para “{{ busqueda }}”
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- PAGINACIÓN -->
            <div class="pager" v-if="totalPages > 1">
                <button class="btn" :disabled="page === 0" @click="primero">« Primero</button>
                <button class="btn" :disabled="page === 0" @click="anterior">← Anterior</button>
                <span>Página {{ page + 1 }} de {{ totalPages }} ({{ totalElements }} registros)</span>
                <button class="btn" :disabled="page >= totalPages - 1" @click="siguiente">Siguiente →</button>
                <button class="btn" :disabled="page >= totalPages - 1" @click="ultimo">Último »</button>
            </div>
        </div>
    </section>

    <ProductoForm v-if="showCreate" @close="onCloseForm" @saved="onSavedProducto" />
</template>
