<template>
    <div class="modal" @keydown.esc="onClose" v-if="true">
        <div class="backdrop" @click="onClose"></div>

        <div class="card modal-card" role="dialog" aria-modal="true">
            <div class="card-header">
                <h3 style="margin:0">Crear cliente</h3>
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
                            <input v-model.trim="form.nombre" placeholder="Nombre *"
                                :class="{ 'invalid': errors.nombre }" autocomplete="off" />
                        </label>
                        <small v-if="errors.nombre" class="err">{{ errors.nombre }}</small>

                        <label class="input">
                            <input v-model.trim="form.ruc" placeholder="RUC" autocomplete="off" />
                        </label>

                        <label class="input">
                            <input v-model.trim="form.telefono" placeholder="Teléfono" autocomplete="off" />
                        </label>

                        <label class="input">
                            <input v-model.trim="form.correo" placeholder="Correo" autocomplete="off"
                                :class="{ 'invalid': errors.correo }" />
                        </label>
                        <small v-if="errors.correo" class="err">{{ errors.correo }}</small>

                        <label class="input" style="grid-column:1/-1">
                            <input v-model.trim="form.direccion" placeholder="Dirección" autocomplete="off" />
                        </label>
                    </div>
                </form>

                <div v-if="serverError" class="error" style="margin-top:8px">{{ serverError }}</div>
            </div>

            <div class="card-header" style="border-top:1px solid var(--border); border-bottom:none">
                <div style="display:flex; gap:8px; margin-left:auto">
                    <button class="btn ghost" :disabled="loading" @click="onClose">Cancelar</button>
                    <button class="btn" :disabled="loading" @click="onSubmit">
                        <span v-if="!loading">Guardar</span>
                        <span v-else>Guardando…</span>
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import "../assets/clienteFrom.css"
import { reactive, ref } from "vue"
import { fetchWithAuth } from "../services/authService"   
// Usamos la misma base que en auth.js para no duplicar
const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080"

const emit = defineEmits(["close", "saved"])

const form = reactive({
    nombre: "",
    ruc: "",
    telefono: "",
    correo: "",
    direccion: "",
})

const loading = ref(false)
const serverError = ref("")
const errors = reactive({ nombre: "", correo: "" })

function validate() {
    errors.nombre = form.nombre ? "" : "El nombre es obligatorio."
    errors.correo =
        form.correo && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.correo)
            ? "Correo inválido."
            : ""
    return !errors.nombre && !errors.correo
}

async function onSubmit() {
    serverError.value = ""
    if (!validate()) return
    loading.value = true

    try {
        const created = await fetchWithAuth(`${API_BASE}/clientes`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(form),
        })

        emit("saved", created)
        reset()
    } catch (e) {
        // Podés personalizar el mensaje si la sesión expiró, etc.
        serverError.value = e?.message || "No se pudo crear el cliente."
    } finally {
        loading.value = false
    }
}

function reset() {
    form.nombre = ""
    form.ruc = ""
    form.telefono = ""
    form.correo = ""
    form.direccion = ""
}

function onClose() {
    emit("close")
}
</script>
