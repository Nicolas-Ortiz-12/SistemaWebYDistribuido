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
                    <input placeholder="Buscar por nombre" v-model="q.nombre" />
                </label>
            </div>

            <CategoriesTable :rows="pagina" @edit="editar" @remove="eliminar" />

            <div style="margin-top:12px">
                <Pagination :total="total" v-model:page="page" v-model:pageSize="pageSize" />
            </div>
        </div>
    </section>
</template>

<script setup>
import { reactive, computed, ref, watch } from 'vue'
import { categories as DATA } from '../../data/categories.js'
import CategoriesTable from '../../components/CategoriesTable.vue'
import Pagination from '../../components/Pagination.vue'

const q = reactive({ nombre: '' })
const list = ref([...DATA])

// filtro
const filtradas = computed(() => {
    const n = q.nombre.toLowerCase()
    return list.value.filter(c => !n || c.nombre.toLowerCase().includes(n))
})

// paginación
const page = ref(1)
const pageSize = ref(10)
const total = computed(() => filtradas.value.length)
const pagina = computed(() => {
    const start = (page.value - 1) * pageSize.value
    return filtradas.value.slice(start, start + pageSize.value)
})

// resetear a la página 1 cuando cambie el filtro o la cantidad de filas
watch(() => [q.nombre, list.value.length], () => { page.value = 1 })

function editar(c) {
    const nuevo = prompt('Editar nombre de la categoría:', c.nombre)
    if (nuevo && nuevo.trim()) c.nombre = nuevo.trim()
}
function eliminar(c) {
    if (confirm(`¿Eliminar la categoría "${c.nombre}"?`)) {
        list.value = list.value.filter(x => x !== c)
    }
}
function nuevaCategoria() {
    const nombre = prompt('Nombre de la nueva categoría:')
    if (nombre && nombre.trim()) list.value.unshift({ nombre: nombre.trim() })
}
</script>
