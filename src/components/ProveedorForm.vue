<template>
    <div class="modal">
        <div class="backdrop" @click="onClose"></div>

        <div class="card modal-card">
            <div class="card-header">
                <h3 style="margin:0">Nuevo proveedor</h3>
                <button class="icon-btn" @click="onClose" title="Cerrar">
                    ✕
                </button>
            </div>

            <div class="card-body">
                <form @submit.prevent="onSubmit" class="form-grid">
                    <label class="input">
                        <input v-model="form.nombre" placeholder="Nombre *" required />
                    </label>

                    <label class="input">
                        <input v-model="form.ruc" placeholder="RUC" />
                    </label>

                    <label class="input">
                        <input v-model="form.telefono" placeholder="Teléfono" />
                    </label>

                    <label class="input">
                        <input v-model="form.correo" placeholder="Correo" type="email" />
                    </label>

                    <label class="input" style="grid-column:1/-1">
                        <input v-model="form.direccion" placeholder="Dirección" />
                    </label>

                    <label class="input">
                        <select v-model="form.activo">
                            <option :value="true">Activo</option>
                            <option :value="false">Inactivo</option>
                        </select>
                    </label>

                    <div class="actions" style="grid-column:1/-1;text-align:right;margin-top:10px">
                        <button type="button" class="btn ghost" @click="onClose">Cancelar</button>
                        <button type="submit" class="btn">Guardar</button>
                    </div>
                </form>
                <div v-if="error" class="error">{{ error }}</div>
            </div>
        </div>
    </div>
</template>

<script setup>
import "../assets/proveedorForm.css"
import { reactive, ref } from 'vue'

const emit = defineEmits(['close', 'saved'])
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const form = reactive({
    nombre: '',
    ruc: '',
    telefono: '',
    correo: '',
    direccion: '',
    activo: true
})

const error = ref('')
const loading = ref(false)

async function onSubmit() {
    error.value = ''
    loading.value = true
    try {
        const resp = await fetch(`${API_URL}/proveedores`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(form)
        })
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
        emit('saved')
    } catch (e) {
        console.error(e)
        error.value = 'No se pudo crear el proveedor.'
    } finally {
        loading.value = false
    }
}

function onClose() {
    emit('close')
}
</script>
