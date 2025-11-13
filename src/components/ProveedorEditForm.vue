<template>
    <div class="modal" @keydown.esc="onClose">
        <div class="backdrop" @click="onClose"></div>

        <div class="card modal-card" role="dialog" aria-modal="true">
            <div class="card-header">
                <h3 style="margin:0">Editar proveedor</h3>
                <button class="icon-btn" @click="onClose" title="Cerrar">✕</button>
            </div>

            <div class="card-body">
                <form @submit.prevent="onSubmit" class="form-grid">
                    <label class="input">
                        <input v-model.trim="form.nombre" placeholder="Nombre *" required />
                    </label>

                    <label class="input">
                        <input v-model.trim="form.ruc" placeholder="RUC" />
                    </label>

                    <label class="input">
                        <input v-model.trim="form.telefono" placeholder="Teléfono" />
                    </label>

                    <label class="input">
                        <input v-model.trim="form.correo" type="email" placeholder="Correo" />
                    </label>

                    <label class="input" style="grid-column:1/-1">
                        <input v-model.trim="form.direccion" placeholder="Dirección" />
                    </label>

                    <label class="input">
                        <select v-model="form.activo">
                            <option :value="true">Activo</option>
                            <option :value="false">Inactivo</option>
                        </select>
                    </label>

                    <div class="actions" style="grid-column:1/-1;text-align:right;margin-top:10px">
                        <button type="button" class="btn ghost" @click="onClose" :disabled="loading">
                            Cancelar
                        </button>
                        <button type="submit" class="btn" :disabled="loading">
                            <span v-if="!loading">Guardar cambios</span>
                            <span v-else>Guardando…</span>
                        </button>
                    </div>
                </form>
                <div v-if="error" class="error">{{ error }}</div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref, watch } from "vue"
import "../assets/ProveedorForm.css"
import { fetchWithAuth } from "../services/authService" // ⬅️ usamos auth service

const props = defineProps({ proveedor: { type: Object, required: true } })
const emit = defineEmits(["close", "updated"])

// Unificamos con lo que usas en auth.js
const API_BASE =
    import.meta.env.VITE_API_BASE ??
    import.meta.env.VITE_API_URL ??
    "http://localhost:8080"

const form = reactive({
    id: null,
    nombre: "",
    ruc: "",
    telefono: "",
    correo: "",
    direccion: "",
    activo: true,
})

const loading = ref(false)
const error = ref("")

watch(
    () => props.proveedor,
    (p) => {
        if (!p) return
        form.id = p.id ?? null
        form.nombre = p.nombre ?? ""
        form.ruc = p.ruc ?? ""
        form.telefono = p.telefono ?? ""
        form.correo = p.correo ?? ""
        form.direccion = p.direccion ?? ""
        form.activo = p.activo ?? true
    },
    { immediate: true }
)

async function onSubmit() {
    error.value = ""
    loading.value = true
    try {
        const updated = await fetchWithAuth(`${API_BASE}/proveedores/${form.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(form),
        })

        // `fetchWithAuth` ya lanza si no es ok, así que si llega acá es éxito
        emit("updated", updated)
    } catch (e) {
        console.error(e)
        error.value = e?.message || "No se pudo actualizar el proveedor."
    } finally {
        loading.value = false
    }
}

function onClose() {
    emit("close")
}
</script>
