<script setup>
import { ref, watch, onMounted, onUnmounted } from "vue"
import "../../assets/productos.css"
import ProductoForm from "../../components/ProductoForm.vue"
import { fetchWithAuth } from "../../services/authService"
import { API_BASE } from "../../services/api"

const busqueda = ref("")
const productos = ref([])
const page = ref(0)
const size = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)
const loading = ref(false)
const error = ref("")

let aborter = null
let debounceId = null

const fmt = new Intl.NumberFormat("es-PY", {
  style: "currency",
  currency: "PYG",
  maximumFractionDigits: 0,
})
const fmtPrecio = (v) => (isFinite(v) ? fmt.format(v) : "0 Gs.")

function normalizeProducto(p = {}) {
  return {
    id: p.id ?? null,
    codigo: p.codigo ?? "",
    nombre: p.nombre ?? "",
    costo: p.costo ?? 0,
    precio: p.precio ?? 0,
    stockMinimo: p.stockMinimo ?? 0,
    unidadMedida: p.unidadMedida ?? "UN",
    descripcion: p.descripcion ?? "",
  }
}

async function fetchProductos() {
  if (aborter) aborter.abort()
  aborter = new AbortController()
  loading.value = true
  error.value = ""

  try {
    const p = page.value
    const s = size.value
    const q = busqueda.value?.trim()

    let endpoint = `${API_BASE}/productos/${p}/${s}`
    if (q) endpoint += `/${encodeURIComponent(q)}`

    const data = await fetchWithAuth(endpoint, { signal: aborter.signal })
    const content = Array.isArray(data?.content)
      ? data.content
      : Array.isArray(data)
        ? data
        : []
    productos.value = content.map(normalizeProducto)
    totalPages.value = data.totalPages ?? 1
    totalElements.value = data.totalElements ?? productos.value.length
  } catch (e) {
    if (e?.name !== "AbortError") {
      console.error(e)
      error.value = e?.message || "No se pudieron cargar los productos."
    }
  } finally {
    loading.value = false
  }
}

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

const showForm = ref(false)
const editingProducto = ref(null)

function nuevoProducto() {
  editingProducto.value = null
  showForm.value = true
}

function abrirEditarProducto(p) {
  editingProducto.value = p
  showForm.value = true
}

async function eliminarProducto(p) {
  if (!confirm(`¿Estás seguro de que deseas eliminar el producto "${p.nombre}"?`)) return
  try {
    loading.value = true
    await fetchWithAuth(`${API_BASE}/productos/${p.id}`, {
      method: "DELETE",
    })
    if (productos.value.length === 1 && page.value > 0) {
      page.value--
    } else {
      await fetchProductos()
    }
  } catch (e) {
    console.error(e)
    error.value = e?.message || "No se pudo eliminar el producto."
  } finally {
    loading.value = false
  }
}

function onCloseForm() {
  showForm.value = false
  editingProducto.value = null
}

function onSavedProducto() {
  showForm.value = false
  editingProducto.value = null
  fetchProductos()
}
</script>

<template>
  <div class="products-page-container">
    <!-- Header Section -->
    <div class="premium-page-header">
      <div class="header-left">
        <h2 class="title premium-title">Catálogo de Productos</h2>
        <p class="page-subtitle">Gestiona y actualiza el inventario en tiempo real con una interfaz fluida.</p>
      </div>
      <button type="button" class="btn btn-primary btn-premium-action" @click="nuevoProducto">
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
        </svg>
        Registrar Producto
      </button>
    </div>

    <!-- Filters & Search Section -->
    <div class="card premium-filters-card">
      <div class="card-body">
        <div class="premium-filters-grid">
          <div class="search-input-wrapper">
            <span class="field-label">Buscar producto</span>
            <div class="input-icon-container">
              <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input type="text" placeholder="Buscar por código, nombre o descripción..." v-model="busqueda" class="premium-search-input" />
            </div>
          </div>

          <div class="size-select-wrapper">
            <span class="field-label">Mostrar</span>
            <select v-model.number="size" :disabled="loading" class="premium-select">
              <option :value="10">10 productos</option>
              <option :value="20">20 productos</option>
              <option :value="50">50 productos</option>
            </select>
          </div>
        </div>
      </div>
    </div>

    <!-- Error Banner -->
    <div v-if="error" class="error-banner fade-in" style="margin-bottom: 20px;">
      <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- Main List Table -->
    <div class="card premium-table-card">
      <div class="card-body p-0">
        <div v-if="loading" class="premium-table-loading">
          <span class="loading-spinner"></span>
          <span>Actualizando catálogo...</span>
        </div>

        <div v-else class="table-responsive">
          <table class="premium-data-table">
            <thead>
              <tr>
                <th style="width: 80px;">Código</th>
                <th>Producto</th>
                <th>Descripción</th>
                <th class="td-right" style="width: 140px;">Costo</th>
                <th class="td-right" style="width: 140px;">Precio</th>
                <th style="width: 110px; text-align: center;">Stock Mín.</th>
                <th style="width: 100px; text-align: center;">Medida</th>
                <th style="width: 120px; text-align: center;">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in productos" :key="p.id" class="premium-row">
                <td class="code-cell">{{ p.codigo || `#${p.id}` }}</td>
                <td class="name-cell">
                  <div class="product-name-txt">{{ p.nombre }}</div>
                </td>
                <td class="desc-cell">{{ p.descripcion || 'Sin descripción' }}</td>
                <td class="td-right value-cell">{{ fmtPrecio(p.costo) }}</td>
                <td class="td-right value-cell price-highlight">{{ fmtPrecio(p.precio) }}</td>
                <td style="text-align: center;" class="value-cell">{{ p.stockMinimo }}</td>
                <td style="text-align: center;">
                  <span class="badge badge-unit">{{ p.unidadMedida }}</span>
                </td>
                <td class="actions-cell">
                  <div class="action-buttons-group">
                    <button type="button" class="action-btn edit-btn" @click="abrirEditarProducto(p)" title="Editar producto">
                      <svg class="action-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                      </svg>
                    </button>
                    <button type="button" class="action-btn delete-btn" @click="eliminarProducto(p)" title="Eliminar producto">
                      <svg class="action-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="productos.length === 0">
                <td colspan="8" class="premium-empty-cell">
                  <div class="empty-state">
                    <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" />
                    </svg>
                    <h4>No se encontraron productos</h4>
                    <p>Intenta ajustar tu búsqueda o crea un producto nuevo.</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Premium Pagination -->
    <div class="premium-pagination-container" v-if="totalPages > 1">
      <div class="pagination-info">
        Mostrando página <strong>{{ page + 1 }}</strong> de <strong>{{ totalPages }}</strong> ({{ totalElements }} productos en total)
      </div>
      <div class="pagination-actions">
        <button type="button" class="btn btn-ghost pagination-btn" :disabled="page === 0" @click="primero" title="Primera página">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
          </svg>
        </button>
        <button type="button" class="btn btn-ghost pagination-btn" :disabled="page === 0" @click="anterior" title="Página anterior">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div class="pagination-current-bubble">{{ page + 1 }}</div>
        <button type="button" class="btn btn-ghost pagination-btn" :disabled="page >= totalPages - 1" @click="siguiente" title="Página siguiente">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
          </svg>
        </button>
        <button type="button" class="btn btn-ghost pagination-btn" :disabled="page >= totalPages - 1" @click="ultimo" title="Última página">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
          </svg>
        </button>
      </div>
    </div>
  </div>

  <ProductoForm v-if="showForm" :producto="editingProducto" @close="onCloseForm" @saved="onSavedProducto" />
</template>
