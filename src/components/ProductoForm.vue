<template>
  <div class="modal fade-in" @keydown.esc="onClose" tabindex="-1" role="dialog">
    <div class="backdrop fade-in-backdrop" @click="onClose"></div>

    <div class="card modal-card scale-in" role="document">
      <div class="card-header premium-header">
        <div class="header-title-area">
          <svg class="header-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>
          <h3>{{ producto ? 'Editar Producto' : 'Nuevo Producto' }}</h3>
        </div>
        <button class="icon-btn close-btn" @click="onClose" title="Cerrar" aria-label="Cerrar">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div class="card-body modal-body-scroll">
        <form @submit.prevent="onSubmit" class="form premium-form">
          <div class="grid">
            <div class="form-field">
              <span class="field-label">Código del Producto</span>
              <label class="input-container">
                <input v-model.trim="form.codigo" placeholder="Ej. SKU-1002" autocomplete="off" ref="firstInput" />
              </label>
            </div>

            <div class="form-field">
              <span class="field-label">Nombre *</span>
              <label class="input-container" :class="{ 'has-error': errors.nombre }">
                <input v-model.trim="form.nombre" placeholder="Nombre descriptivo" :class="{ invalid: errors.nombre }" />
              </label>
              <span v-if="errors.nombre" class="field-error-text">{{ errors.nombre }}</span>
            </div>

            <div class="form-field">
              <span class="field-label">Costo (Compra)</span>
              <label class="input-container">
                <input v-model.number="form.costo" type="number" min="0" step="0.01" placeholder="0.00" />
                <span class="input-suffix">Gs.</span>
              </label>
            </div>

            <div class="form-field">
              <span class="field-label">Precio (Venta)</span>
              <label class="input-container">
                <input v-model.number="form.precio" type="number" min="0" step="0.01" placeholder="0.00" />
                <span class="input-suffix">Gs.</span>
              </label>
            </div>

            <div class="form-field">
              <span class="field-label">Stock Mínimo</span>
              <label class="input-container">
                <input v-model.number="form.stockMinimo" type="number" min="0" step="1" placeholder="0" />
              </label>
            </div>

            <div class="form-field">
              <span class="field-label">Unidad de Medida</span>
              <label class="input-container">
                <input v-model.trim="form.unidadMedida" placeholder="Ej. UN, KG, MT" />
              </label>
            </div>

            <div class="form-field full">
              <span class="field-label">Descripción</span>
              <label class="input-container">
                <input v-model.trim="form.descripcion" placeholder="Detalles adicionales del producto" autocomplete="off" />
              </label>
            </div>
          </div>
        </form>

        <div v-if="serverError" class="error-banner fade-in">
          <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <span>{{ serverError }}</span>
        </div>
      </div>

      <div class="card-footer premium-footer">
        <button class="btn ghost btn-premium-cancel" :disabled="loading" @click="onClose">Cancelar</button>
        <button class="btn btn-primary btn-premium-save" :disabled="loading" @click="onSubmit">
          <span v-if="!loading" class="btn-content">
            <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>
            {{ producto ? 'Guardar Cambios' : 'Registrar Producto' }}
          </span>
          <span v-else class="btn-content">
            <span class="spinner"></span>
            Procesando...
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import "../assets/productoForm.css"
import { reactive, ref, onMounted } from "vue"
import { fetchWithAuth } from "../services/authService"
import { API_BASE } from "../services/api"

const props = defineProps({
  producto: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(["close", "saved"])

const firstInput = ref(null)

const form = reactive({
  id: null,
  codigo: "",
  nombre: "",
  costo: null,
  precio: null,
  stockMinimo: null,
  descripcion: "",
  unidadMedida: "UN",
})

const errors = reactive({ nombre: "" })
const serverError = ref("")
const loading = ref(false)

onMounted(() => {
  if (firstInput.value) {
    firstInput.value.focus()
  }

  if (props.producto) {
    form.id = props.producto.id
    form.codigo = props.producto.codigo || ""
    form.nombre = props.producto.nombre || ""
    form.costo = props.producto.costo ?? null
    form.precio = props.producto.precio ?? null
    form.stockMinimo = props.producto.stockMinimo ?? null
    form.descripcion = props.producto.descripcion || ""
    form.unidadMedida = props.producto.unidadMedida || "UN"
  }
})

function validate() {
  errors.nombre = form.nombre ? "" : "El nombre es obligatorio."
  return !errors.nombre
}

async function onSubmit() {
  if (!validate()) return
  serverError.value = ""
  loading.value = true
  try {
    const isEdit = !!props.producto
    const url = isEdit ? `${API_BASE}/productos/${form.id}` : `${API_BASE}/productos`
    const method = isEdit ? "PUT" : "POST"

    const saved = await fetchWithAuth(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(form),
    })

    emit("saved", saved)
  } catch (e) {
    console.error(e)
    serverError.value = e?.message || "No se pudo guardar el producto."
  } finally {
    loading.value = false
  }
}

function onClose() {
  emit("close")
}
</script>
