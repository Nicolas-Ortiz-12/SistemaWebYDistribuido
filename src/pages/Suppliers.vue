<template>
    <section class="card">
        <div class="card-header">
            <h2 style="margin:0;font-size:18px">Lista de proveedores</h2>
            <button class="btn" @click="nuevoProveedor">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                Proveedor nuevo
            </button>
        </div>

        <div class="card-body">
            <div class="filters">
                <label class="input">
                    <input placeholder="Buscar por nombre" v-model="q" autocomplete="off" />
                </label>

                <label class="input" style="max-width:160px">
                    <select v-model.number="size" :disabled="loading" class="select" title="Items por página">
                        <option :value="10">10 por página</option>
                        <option :value="20">20 por página</option>
                        <option :value="50">50 por página</option>
                    </select>
                </label>
            </div>

            <div v-if="error" class="error">{{ error }}</div>
            <div v-if="loading" class="loading">Cargando…</div>

            <div class="table-wrap">
                <SuppliersTable v-if="!loading" :rows="rows" @edit="editar" @remove="eliminar" />
            </div>

            <div class="pager" v-if="totalPages > 1">
                <button class="btn" :disabled="page === 0 || loading" @click="goFirst">«</button>
                <button class="btn" :disabled="page === 0 || loading" @click="prev">Anterior</button>
                <span>Página {{ page + 1 }} de {{ totalPages }} ({{ totalElements }} total)</span>
                <button class="btn" :disabled="page >= totalPages - 1 || loading" @click="next">Siguiente</button>
                <button class="btn" :disabled="page >= totalPages - 1 || loading" @click="goLast">»</button>
            </div>
        </div>
    </section>

    <!-- Modal Crear -->
    <ProveedorForm v-if="showCreate" @close="onCloseForm" @saved="onSavedProveedor" />

   
    <ProveedorEditForm v-if="showEdit" :proveedor="proveedorSeleccionado" @close="onCloseEdit"
        @updated="onUpdatedProveedor" />
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import SuppliersTable from '../components/SuppliersTable.vue'
import ProveedorForm from '../components/ProveedorForm.vue'
import ProveedorEditForm from '../components/ProveedorEditForm.vue'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'


const q = ref('')
const page = ref(0)
const size = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const rows = ref([])
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const showEdit = ref(false)
const proveedorSeleccionado = ref(null) 

let aborter = null
let debounceId = null

function normalizeProveedor(p = {}) {
    return {
        id: p.id ?? null,
        nombre: p.nombre ?? '',
        ruc: p.ruc ?? '',
        telefono: p.telefono ?? '',
        correo: p.correo ?? '',
        direccion: p.direccion ?? '',
        activo: p.activo ?? true
    }
}

async function fetchProveedores() {
    if (aborter) aborter.abort()
    aborter = new AbortController()
    loading.value = true
    error.value = ''

    try {
        const p = page.value      
        const s = size.value
        const term = q.value?.trim()

        let endpoint = `${API_URL}/proveedores/${p}/${s}`
        if (term) endpoint += `/${encodeURIComponent(term)}`

        const resp = await fetch(endpoint, { signal: aborter.signal })
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`)

        const data = await resp.json()

        
        const content = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : [])
        rows.value = content.map(normalizeProveedor)
        totalPages.value = Number.isInteger(data?.totalPages) ? data.totalPages : 1
        totalElements.value = Number.isInteger(data?.totalElements) ? data.totalElements : rows.value.length

        if (Number.isInteger(data?.number)) page.value = data.number
        if (Number.isInteger(data?.size)) size.value = data.size
    } catch (e) {
        if (e.name !== 'AbortError') {
            console.error(e)
            error.value = 'No se pudieron cargar los proveedores.'
        }
    } finally {
        loading.value = false
    }
}

watch(q, () => {
    page.value = 0
    clearTimeout(debounceId)
    debounceId = setTimeout(fetchProveedores, 350)
})

watch([page, size], () => {
    clearTimeout(debounceId)
    debounceId = setTimeout(fetchProveedores, 0)
})

onMounted(fetchProveedores)

// Navegación
function prev() { if (page.value > 0) page.value-- }
function next() { if (page.value < totalPages.value - 1) page.value++ }
function goFirst() { page.value = 0 }
function goLast() { page.value = Math.max(0, totalPages.value - 1) }

// Crear
function nuevoProveedor() { showCreate.value = true }
function onCloseForm() { showCreate.value = false }
function onSavedProveedor() { showCreate.value = false; fetchProveedores() }

// Eliminar
async function eliminar(p) {
    if (!confirm(`¿Eliminar proveedor "${p.nombre}"?`)) return
    try {
        const resp = await fetch(`${API_URL}/proveedores/${p.id}`, { method: 'DELETE' })
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
        fetchProveedores()
    } catch (e) {
        console.error(e)
        alert('Error al eliminar proveedor')
    }
}

function editar(p) {
    proveedorSeleccionado.value = p
    showEdit.value = true
}
function onCloseEdit() { showEdit.value = false }
function onUpdatedProveedor() {
    showEdit.value = false
    fetchProveedores()
}
</script>
