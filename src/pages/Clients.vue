<template>
    <section class="card">
        <div class="card-header">
            <h2 style="margin:0;font-size:18px">Todos los clientes</h2>
            <button class="btn" @click="nuevoCliente">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                Cliente nuevo
            </button>
        </div>

        <div class="card-body">
            <div class="filters">
                <label class="input">
                    <input placeholder="Buscar (nombre, correo o teléfono)" v-model="q" autocomplete="off" />
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
                <ClientsTable v-if="!loading" :rows="uiRows" @edit="editar" />
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

    <!-- Modales -->
    <ClienteForm v-if="showCreate" @close="onCloseForm" @saved="onSavedCliente" />
    <ClienteEditForm v-if="showEdit" :cliente="clienteSeleccionado" @close="onCloseEdit" @updated="onUpdatedCliente" />
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import ClientsTable from '../components/ClientsTable.vue'
import ClienteForm from '../components/ClienteForm.vue'
import ClienteEditForm from '../components/ClienteEditForm.vue'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// Estado
const q = ref('')
const page = ref(0)
const size = ref(10)

const totalPages = ref(0)
const totalElements = ref(0)
const rows = ref([]) // crudos desde backend

const loading = ref(false)
const error = ref('')

// Crear
const showCreate = ref(false)

// Editar (👈 FALTABAN ESTOS DOS)
const showEdit = ref(false)
const clienteSeleccionado = ref(null)

let aborter = null
let debounceId = null

// Normalizador: asegura el shape coherente con tu BD
function normalizeCliente(c = {}) {
    return {
        id: c.id ?? null,
        nombre: (c.nombre ?? '').toString(),
        ruc: (c.ruc ?? '').toString(),
        telefono: (c.telefono ?? '').toString(),
        correo: (c.correo ?? c.email ?? '').toString(),
        direccion: (c.direccion ?? '').toString(),
    }
}

// Filas preparadas para la tabla
const uiRows = computed(() => rows.value.map(normalizeCliente))

async function fetchClientes() {
    if (aborter) aborter.abort()
    aborter = new AbortController()

    loading.value = true
    error.value = ''
    try {
        const url = new URL(`${API_URL}/clientes`)
        url.searchParams.set('page', page.value)
        url.searchParams.set('size', size.value)
        if (q.value?.trim()) url.searchParams.set('q', q.value.trim())

        const resp = await fetch(url.toString(), { signal: aborter.signal })
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`)

        const data = await resp.json()
        rows.value = Array.isArray(data.content) ? data.content : []
        totalPages.value = Number.isInteger(data.totalPages) ? data.totalPages : 1
        totalElements.value = Number.isInteger(data.totalElements) ? data.totalElements : rows.value.length
        if (Number.isInteger(data.number)) page.value = data.number
    } catch (e) {
        if (e.name !== 'AbortError') {
            console.error(e)
            error.value = 'No se pudo cargar la lista de clientes.'
        }
    } finally {
        loading.value = false
    }
}

// Debounce búsqueda
watch(q, () => {
    page.value = 0
    clearTimeout(debounceId)
    debounceId = setTimeout(fetchClientes, 350)
})

// Paginación / tamaño
watch([page, size], () => {
    clearTimeout(debounceId)
    debounceId = setTimeout(fetchClientes, 0)
})

onMounted(fetchClientes)
onUnmounted(() => {
    if (aborter) aborter.abort()
    clearTimeout(debounceId)
})

// Navegación
function prev() { if (page.value > 0) page.value-- }
function next() { if (page.value < totalPages.value - 1) page.value++ }
function goFirst() { page.value = 0 }
function goLast() { page.value = Math.max(0, totalPages.value - 1) }

// Crear
function nuevoCliente() { showCreate.value = true }
function onCloseForm() { showCreate.value = false }
function onSavedCliente() {
    showCreate.value = false
    fetchClientes()
}

// Editar
function editar(c) {
    clienteSeleccionado.value = c
    showEdit.value = true
}
function onCloseEdit() { showEdit.value = false }
function onUpdatedCliente() {
    showEdit.value = false
    fetchClientes()
}
</script>
