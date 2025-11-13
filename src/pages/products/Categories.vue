<template>
    <section class="card">
        <div class="card-header">
            <h2 style="margin:0;font-size:18px">Lista de categorías</h2>
            <button class="btn" @click="nuevaCategoria">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                Nueva categoría
            </button>
        </div>

        <div class="card-body">
            <div class="filters">
                <label class="input">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <circle cx="11" cy="11" r="8" />
                        <line x1="21" y1="21" x2="16.65" y2="16.65" />
                    </svg>
                    <input placeholder="Buscar por nombre" v-model="q.nombre" autocomplete="off" />
                </label>
            </div>

            <div v-if="error" class="error">{{ error }}</div>
            <div v-if="loading" class="loading">Cargando…</div>

            <CategoriesTable v-if="!loading" :rows="rows" @edit="editar" @remove="eliminar" />

            <div style="margin-top:12px">
                <Pagination :total="total" v-model:page="page" v-model:pageSize="pageSize" />
            </div>
        </div>
    </section>
</template>

<script setup>
import { reactive, ref, watch, onMounted, onUnmounted } from "vue"
import CategoriesTable from "../../components/CategoriesTable.vue"
import Pagination from "../../components/Pagination.vue"
import { fetchWithAuth } from "../../services/authService" 

// Base de API unificada con el auth service
const API_BASE = (
    import.meta.env.VITE_API_BASE ??
    import.meta.env.VITE_API_URL ??
    "http://localhost:8080"
).replace(/\/+$/, "")

function buildQuery(params = {}) {
    const u = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && String(v).trim() !== "") u.set(k, v)
    })
    const s = u.toString()
    return s ? `?${s}` : ""
}

const categoriaApi = {
    async listPaged({ page, size, q, signal }) {
        // Tu Swagger: GET /categorias/{page}/{size} con q opcional
        const path = `${API_BASE}/categorias/${page}/${size}${buildQuery({ q })}`
        // fetchWithAuth ya parsea JSON o lanza Error con mensaje legible
        return fetchWithAuth(path, { signal })
    },
    async create({ nombre }) {
        const resp = await fetchWithAuth(`${API_BASE}/categorias`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ nombre }),
        })
        return resp ?? null
    },
    async update(id, payload) {
        const resp = await fetchWithAuth(`${API_BASE}/categorias/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        })
        return resp ?? null
    },
    async remove(id) {
        await fetchWithAuth(`${API_BASE}/categorias/${id}`, {
            method: "DELETE",
        })
        return true
    },
}

const q = reactive({ nombre: "" })

// Paginación (UI 1-based; backend 0-based)
const page = ref(1)
const pageSize = ref(10)

// Datos
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref("")

// Control de abort/debounce
let aborter = null
let debounceId = null

async function fetchCategorias() {
    if (aborter) aborter.abort()
    aborter = new AbortController()

    loading.value = true
    error.value = ""
    try {
        const p0 = Math.max(0, page.value - 1)
        const s = pageSize.value
        const data = await categoriaApi.listPaged({
            page: p0,
            size: s,
            q: q.nombre?.trim() || undefined,
            signal: aborter.signal,
        })

        // Soporta Page<> de Spring y arreglo plano
        rows.value = Array.isArray(data?.content)
            ? data.content
            : Array.isArray(data)
                ? data
                : []

        total.value = Number.isInteger(data?.totalElements)
            ? data.totalElements
            : rows.value.length

        if (Number.isInteger(data?.number)) page.value = data.number + 1
        if (Number.isInteger(data?.size)) pageSize.value = data.size
    } catch (e) {
        if (e.name !== "AbortError") {
            console.error(e)
            error.value =
                typeof e?.message === "string"
                    ? e.message
                    : "No se pudo cargar la lista de categorías."
        }
    } finally {
        loading.value = false
    }
}

// Debounce de búsqueda → vuelve a página 1
watch(
    () => q.nombre,
    () => {
        page.value = 1
        clearTimeout(debounceId)
        debounceId = setTimeout(fetchCategorias, 350)
    }
)

// Cambios de página/tamaño
watch([page, pageSize], () => {
    clearTimeout(debounceId)
    debounceId = setTimeout(fetchCategorias, 0)
})

onMounted(fetchCategorias)
onUnmounted(() => {
    clearTimeout(debounceId)
    if (aborter) aborter.abort()
})

async function nuevaCategoria() {
    const nombre = prompt("Nombre de la nueva categoría:")
    if (!nombre || !nombre.trim()) return
    try {
        loading.value = true
        await categoriaApi.create({ nombre: nombre.trim() })
        await fetchCategorias()
    } catch (e) {
        console.error(e)
        error.value =
            typeof e?.message === "string"
                ? e.message
                : "No se pudo crear la categoría."
    } finally {
        loading.value = false
    }
}

async function editar(c) {
    const nuevo = prompt("Editar nombre de la categoría:", c.nombre)
    if (!nuevo || !nuevo.trim()) return
    try {
        loading.value = true
        await categoriaApi.update(c.id, { ...c, nombre: nuevo.trim() })
        await fetchCategorias()
    } catch (e) {
        console.error(e)
        error.value =
            typeof e?.message === "string"
                ? e.message
                : "No se pudo actualizar la categoría."
    } finally {
        loading.value = false
    }
}

async function eliminar(c) {
    if (!confirm(`¿Eliminar la categoría "${c.nombre}"?`)) return
    try {
        loading.value = true
        await categoriaApi.remove(c.id)
        // Mantener página coherente si borras el último de la página
        if (rows.value.length === 1 && page.value > 1) {
            page.value = page.value - 1
        } else {
            await fetchCategorias()
        }
    } catch (e) {
        console.error(e)
        error.value =
            typeof e?.message === "string"
                ? e.message
                : "No se pudo eliminar la categoría."
    } finally {
        loading.value = false
    }
}
</script>
