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
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <circle cx="11" cy="11" r="8" />
                        <line x1="21" y1="21" x2="16.65" y2="16.65" />
                    </svg>
                    <input placeholder="Nombre" v-model="q.nombre" />
                </label>
                <label class="input">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M4 4h16v16H4z" />
                        <path d="M22 6l-10 7L2 6" />
                    </svg>
                    <input placeholder="Correo electrónico" v-model="q.correo" />
                </label>
                <label class="input">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path
                            d="M22 16.92V21a2 2 0 0 1-2.18 2A19.79 19.79 0 0 1 3 5.18 2 2 0 0 1 5 3h4.09a2 2 0 0 1 2 1.72c.12.81.3 1.6.57 2.35a2 2 0 0 1-.45 2.11L10.91 10.91a16 16 0 0 0 6.18 6.18l1.73-1.73a2 2 0 0 1 2.11-.45c.75.27 1.54.45 2.35.57A2 2 0 0 1 22 16.92z" />
                    </svg>
                    <input placeholder="Teléfono" v-model="q.telefono" />
                </label>
            </div>


            <SuppliersTable :rows="filtrados" @edit="editar" @remove="eliminar" />
        </div>
    </section>
</template>


<script setup>
import { reactive, computed, ref } from 'vue'
import { suppliers as DATA } from '../data/suppliers.js'
import SuppliersTable from '../components/SuppliersTable.vue'


const q = reactive({ nombre: '', correo: '', telefono: '' })
const list = ref([...DATA])


const filtrados = computed(() => {
    const n = q.nombre.toLowerCase()
    const e = q.correo.toLowerCase()
    const t = q.telefono.toLowerCase()
    return list.value.filter(s =>
        (!n || s.nombre.toLowerCase().includes(n)) &&
        (!e || s.email.toLowerCase().includes(e)) &&
        (!t || s.telefono.toLowerCase().includes(t))
    )
})


function editar(s) { alert('Editar: ' + s.nombre) }
function eliminar(s) {
    if (confirm(`¿Eliminar proveedor "${s.nombre}"?`)) {
        list.value = list.value.filter(x => x.email !== s.email)
    }
}
function nuevoProveedor() { alert('Crear nuevo proveedor') }
</script>