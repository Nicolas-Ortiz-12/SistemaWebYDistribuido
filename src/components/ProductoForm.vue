
<template>
    <div class="modal" @keydown.esc="onClose">
        <div class="backdrop" @click="onClose"></div>

        <div class="card modal-card" role="dialog" aria-modal="true">
            <div class="card-header">
                <h3 style="margin:0">Nuevo producto</h3>
                <button class="icon-btn" @click="onClose" title="Cerrar" aria-label="Cerrar">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                </button>
            </div>

            <div class="card-body">
                <form @submit.prevent="onSubmit" class="form">
                    <div class="grid">
                        <label class="input">
                            <input v-model.trim="form.codigo" placeholder="Código" autocomplete="off" />
                        </label>

                        <label class="input">
                            <input v-model.trim="form.nombre" placeholder="Nombre *"
                                :class="{ invalid: errors.nombre }" />
                        </label>

                        <!-- 🔽 Selector con búsqueda de categorías (typeahead) -->
                        <div class="category-combobox" style="position:relative;">
                            <label class="input" style="position:relative; z-index:2;">
                                <input v-model.trim="categoryQuery" placeholder="Buscar categoría…"
                                    @focus="openCat = true" @input="onTypeCategory" autocomplete="off" />
                                <!-- Muestra la selección actual (id) a la derecha, opcional -->
                                <span v-if="form.categoriaId" style="font-size:12px;color:var(--muted)">#{{
                                    form.categoriaId }}</span>
                            </label>

                            <div v-if="openCat && filteredCategories.length" class="dropdown"
                                style="position:absolute; z-index:3; left:0; right:0; background:#fff; border:1px solid var(--border); border-radius:8px; max-height:220px; overflow:auto; margin-top:6px; box-shadow:0 8px 20px rgba(0,0,0,.06);">
                                <button v-for="cat in filteredCategories" :key="cat.id" type="button" class="option"
                                    @click="selectCategory(cat)"
                                    style="display:flex; justify-content:space-between; width:100%; padding:8px 10px; background:#fff; border:none; cursor:pointer; text-align:left;">
                                    <span>{{ cat.nombre }}</span>
                                    <small style="color:var(--muted)">#{{ cat.id }}</small>
                                </button>
                                <div v-if="loadingCats" style="padding:8px 10px; color:var(--muted);">Buscando…</div>
                            </div>

                            <small v-if="categoryError" class="err">{{ categoryError }}</small>
                        </div>

                        <label class="input">
                            <input v-model.number="form.costo" type="number" min="0" step="0.01" placeholder="Costo" />
                        </label>

                        <label class="input">
                            <input v-model.number="form.precio" type="number" min="0" step="0.01"
                                placeholder="Precio" />
                        </label>

                        <label class="input">
                            <input v-model.number="form.stockMinimo" type="number" min="0" step="1"
                                placeholder="Stock mínimo" />
                        </label>

                        <label class="input">
                            <input v-model.trim="form.unidadMedida" placeholder="Unidad de medida (UN, KG…)" />
                        </label>

                        <label class="input" style="grid-column: 1 / -1">
                            <input v-model.trim="form.descripcion" placeholder="Descripción" autocomplete="off" />
                        </label>
                    </div>
                </form>

                <div v-if="serverError" class="error" style="margin-top:8px">{{ serverError }}</div>
            </div>

            <div class="card-header" style="border-top:1px solid var(--border); border-bottom:none">
                <div style="display:flex; gap:8px; margin-left:auto">
                    <button class="btn ghost" :disabled="loading" @click="onClose">Cancelar</button>
                    <button class="btn" :disabled="loading" @click="onSubmit">
                        <span v-if="!loading">Guardar producto</span>
                        <span v-else>Guardando…</span>
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import "../assets/productoForm.css"
import { reactive, ref, computed, watch, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['close', 'saved'])
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const form = reactive({
    codigo: '',
    nombre: '',
    categoriaId: null, 
    costo: null,
    precio: null,
    stockMinimo: null,
    descripcion: '',
    unidadMedida: ''
})

const errors = reactive({ nombre: '' })
const serverError = ref('')
const loading = ref(false)

// ---- Categorías (combo con búsqueda) ----
const categories = ref([])     // cache de resultados recientes
const categoryQuery = ref('')  // lo que escribe el usuario
const openCat = ref(false)
const loadingCats = ref(false)
const categoryError = ref('')
let catAborter = null
let catDebounceId = null

const filteredCategories = computed(() => {
    const q = categoryQuery.value.trim().toLowerCase()
    if (!q) return categories.value
    return categories.value.filter(c => c.nombre?.toLowerCase().includes(q))
})

function selectCategory(cat) {
    form.categoriaId = cat.id
    categoryQuery.value = cat.nombre
    openCat.value = false
}

async function fetchCategorias() {
    if (catAborter) catAborter.abort()
    catAborter = new AbortController()
    loadingCats.value = true
    categoryError.value = ''
    try {
        const url = new URL(`${API_URL}/categorias`)
        url.searchParams.set('page', 0)
        url.searchParams.set('size', 50) // ajusta según tu volumen
        if (categoryQuery.value?.trim()) url.searchParams.set('q', categoryQuery.value.trim())

        const resp = await fetch(url.toString(), { signal: catAborter.signal })
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
        const data = await resp.json()
        categories.value = Array.isArray(data.content) ? data.content : []
    } catch (e) {
        if (e.name !== 'AbortError') {
            console.error(e)
            categoryError.value = 'No se pudieron cargar categorías.'
        }
    } finally {
        loadingCats.value = false
    }
}

// precarga inicial
onMounted(() => {
    fetchCategorias()
    // cerrar dropdown al hacer clic afuera
    window.addEventListener('click', onClickOutside, true)
})
onUnmounted(() => {
    if (catAborter) catAborter.abort()
    clearTimeout(catDebounceId)
    window.removeEventListener('click', onClickOutside, true)
})

// cuando se tipea, busca con debounce
function onTypeCategory() {
    clearTimeout(catDebounceId)
    openCat.value = true
    catDebounceId = setTimeout(fetchCategorias, 300)
}

function onClickOutside(e) {
    // si el click no fue dentro del combobox, cerramos
    const el = e.target.closest('.category-combobox')
    if (!el) openCat.value = false
}


function validate() {
    errors.nombre = form.nombre ? '' : 'El nombre es obligatorio.'
    if (!form.categoriaId) {
        categoryError.value = 'Seleccioná una categoría.'
    } else {
        categoryError.value = ''
    }
    return !errors.nombre && !categoryError.value
}

async function onSubmit() {
    if (!validate()) return
    serverError.value = ''
    loading.value = true
    try {
        const resp = await fetch(`${API_URL}/productos`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(form)
        })
        if (!resp.ok) {
            let msg = `Error HTTP ${resp.status}`
            try {
                const data = await resp.json()
                if (data?.message) msg = data.message
            } catch (_) { }
            throw new Error(msg)
        }
        const created = await resp.json()
        emit('saved', created)
    } catch (e) {
        console.error(e)
        serverError.value = e.message || 'No se pudo guardar el producto.'
    } finally {
        loading.value = false
    }
}

function onClose() { emit('close') }
</script>
