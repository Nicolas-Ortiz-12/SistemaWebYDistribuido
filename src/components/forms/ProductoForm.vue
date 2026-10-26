<template>
    <BaseModal :title="producto ? 'Editar producto' : 'Nuevo producto'" :icon="IconPackage" size="lg" :busy="saving"
        @close="emit('close')">
        <form :id="formId" class="form-grid" novalidate @submit.prevent="submit">
            <FormField v-slot="f" label="Código" required :error="errors.codigo">
                <input :id="f.id" v-model="form.codigo" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.codigo" autocomplete="off" spellcheck="false" />
            </FormField>
            <FormField v-slot="f" label="Nombre" required :error="errors.nombre">
                <input :id="f.id" v-model="form.nombre" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.nombre" autocomplete="off" />
            </FormField>
            <FormField v-slot="f" label="Categoría" :error="errors.categoriaId">
                <SearchSelect v-model="form.categoriaId" :input-id="f.id" :selected-label="categoriaLabel"
                    :search="searchCategorias" placeholder="Buscar categoría…" :invalid="f.invalid"
                    @select="opt => (categoriaLabel = opt?.label ?? '')" />
            </FormField>
            <FormField v-slot="f" label="Unidad de medida" required :error="errors.unidadMedida">
                <select :id="f.id" v-model="form.unidadMedida" class="select" :class="{ invalid: f.invalid }">
                    <option v-for="u in unidades" :key="u" :value="u">{{ u }}</option>
                </select>
            </FormField>
            <FormField v-slot="f" label="Costo" :error="errors.costo">
                <input :id="f.id" v-model.number="form.costo" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" type="number" min="0" step="any" inputmode="decimal" />
            </FormField>
            <FormField v-slot="f" label="Precio de venta" required :error="errors.precio" :hint="precioHint">
                <input :id="f.id" v-model.number="form.precio" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" type="number" min="0" step="any" inputmode="decimal" />
            </FormField>
            <FormField v-slot="f" :label="producto ? 'Stock actual' : 'Stock inicial'" :error="errors.stockMinimo"
                :hint="producto ? 'Se actualiza con las ventas y las compras.' : ''">
                <input :id="f.id" v-model.number="form.stockMinimo" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" type="number" min="0" step="any" inputmode="decimal"
                    :readonly="!!producto" />
            </FormField>
            <FormField v-slot="f" label="Descripción" :error="errors.descripcion" class="span-all">
                <textarea :id="f.id" v-model="form.descripcion" class="textarea" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.descripcion" rows="2" />
            </FormField>
            <AlertBox v-if="serverError" type="error" class="span-all">{{ serverError }}</AlertBox>
        </form>

        <template #footer>
            <button type="button" class="btn" :disabled="saving" @click="emit('close')">Cancelar</button>
            <button type="submit" class="btn btn-primary" :form="formId" :disabled="saving">
                <IconLoader2 v-if="saving" :size="16" class="spin" />
                <IconDeviceFloppy v-else :size="16" />
                Guardar
            </button>
        </template>
    </BaseModal>
</template>

<script setup>
import { computed, onMounted, ref, useId } from 'vue'
import { IconDeviceFloppy, IconLoader2, IconPackage } from '@tabler/icons-vue'
import BaseModal from '../ui/BaseModal.vue'
import FormField from '../ui/FormField.vue'
import AlertBox from '../ui/AlertBox.vue'
import SearchSelect from '../ui/SearchSelect.vue'
import { productosApi } from '../../api/resources'
import { catalogs } from '../../api/catalogs'
import { searchCategorias } from '../../api/searchers'
import { useEntityForm } from '../../composables/useEntityForm'
import { formatMoney } from '../../utils/format'
import { LIMITS, rules } from '../../utils/validation'

const props = defineProps({ producto: { type: Object, default: null } })
const emit = defineEmits(['close', 'saved'])

const L = LIMITS.producto
const formId = useId()
const UNIDADES = ['UN', 'KG', 'G', 'M', 'CM', 'M2', 'L', 'ML', 'CAJA', 'PAR', 'ROLLO', 'BOLSA', 'JUEGO']
const p = props.producto

const { form, errors, saving, serverError, submit } = useEntityForm({
    initial: {
        codigo: p?.codigo ?? '',
        nombre: p?.nombre ?? '',
        categoriaId: p?.categoriaId ?? null,
        unidadMedida: p?.unidadMedida || 'UN',
        costo: p?.costo ?? 0,
        precio: p?.precio ?? 0,
        stockMinimo: p?.stockMinimo ?? 0,
        descripcion: p?.descripcion ?? '',
    },
    schema: {
        codigo: [rules.required(), rules.maxLength(L.codigo), rules.code()],
        nombre: [rules.required(), rules.maxLength(L.nombre)],
        unidadMedida: [rules.required(), rules.maxLength(L.unidadMedida)],
        costo: [rules.number({ min: 0 })],
        precio: [rules.required(), rules.number({ min: 0 })],
        stockMinimo: [rules.number({ min: 0 })],
        descripcion: [rules.maxLength(L.descripcion)],
    },
    save: data => {
        const body = { ...data, costo: Number(data.costo) || 0, precio: Number(data.precio) || 0, stockMinimo: Number(data.stockMinimo) || 0 }
        return p ? productosApi.update(p.id, body) : productosApi.create(body)
    },
    onSaved: saved => {
        catalogs.productos.invalidate()
        emit('saved', saved)
    },
})

// Un valor guardado que no está en la lista se mantiene como opción
const unidades = computed(() => (UNIDADES.includes(form.unidadMedida) ? UNIDADES : [form.unidadMedida, ...UNIDADES]))

const precioHint = computed(() =>
    Number(form.precio) < Number(form.costo) ? `Atención: el precio es menor que el costo (${formatMoney(form.costo)}).` : '')

const categoriaLabel = ref('')
onMounted(async () => {
    if (form.categoriaId == null) return
    try {
        const { byId } = await catalogs.categorias.load()
        categoriaLabel.value = byId.get(form.categoriaId)?.nombre ?? `#${form.categoriaId}`
    } catch {
        categoriaLabel.value = `#${form.categoriaId}`
    }
})
</script>
