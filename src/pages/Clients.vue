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
                    <input placeholder="Buscar por nombre" v-model="q.nombre" />
                </label>
                <label class="input">
                    <input placeholder="Buscar por correo" v-model="q.correo" />
                </label>
                <label class="input">
                    <input placeholder="Buscar por teléfono" v-model="q.telefono" />
                </label>
            </div>

            <ClientsTable :rows="filtrados" :money="money" @edit="editar" />
        </div>
    </section>
</template>

<script setup>
import { reactive, computed } from 'vue'
import { clients as DATA } from '../data/clients.js'
import { money } from '../utils/format.js'
import ClientsTable from '../components/ClientsTable.vue'

const q = reactive({ nombre: '', correo: '', telefono: '' })

const filtrados = computed(() => {
    const n = q.nombre.toLowerCase()
    const e = q.correo.toLowerCase()
    const t = q.telefono.toLowerCase()
    return DATA.filter(c =>
        (!n || c.nombre.toLowerCase().includes(n)) &&
        (!e || c.email.toLowerCase().includes(e)) &&
        (!t || c.telefono.toLowerCase().includes(t))
    )
})

function editar(c) { alert('Editar: ' + c.nombre) }
function nuevoCliente() { alert('Crear nuevo cliente') }
</script>