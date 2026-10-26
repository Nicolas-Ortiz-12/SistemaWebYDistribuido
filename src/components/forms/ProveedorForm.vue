<template>
    <BaseModal :title="proveedor ? 'Editar proveedor' : 'Nuevo proveedor'" :icon="IconTruck" :busy="saving"
        @close="emit('close')">
        <form :id="formId" class="form-grid" novalidate @submit.prevent="submit">
            <FormField v-slot="f" label="Razón social" required :error="errors.nombre" class="span-all">
                <input :id="f.id" v-model="form.nombre" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.nombre" autocomplete="off" />
            </FormField>
            <FormField v-slot="f" label="RUC" :error="errors.ruc">
                <input :id="f.id" v-model="form.ruc" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.ruc" inputmode="numeric" autocomplete="off"
                    placeholder="80012345-6" />
            </FormField>
            <FormField v-slot="f" label="Teléfono" :error="errors.telefono">
                <input :id="f.id" v-model="form.telefono" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.telefono" type="tel" autocomplete="off" />
            </FormField>
            <FormField v-slot="f" label="Correo electrónico" :error="errors.correo" class="span-all">
                <input :id="f.id" v-model="form.correo" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.correo" type="email" autocomplete="off" />
            </FormField>
            <FormField v-slot="f" label="Dirección" :error="errors.direccion" class="span-all">
                <input :id="f.id" v-model="form.direccion" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.direccion" autocomplete="off" />
            </FormField>
            <FormField v-slot="f" label="Estado">
                <select :id="f.id" v-model="form.activo" class="select">
                    <option :value="true">Activo</option>
                    <option :value="false">Inactivo</option>
                </select>
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
import { useId } from 'vue'
import { IconDeviceFloppy, IconLoader2, IconTruck } from '@tabler/icons-vue'
import BaseModal from '../ui/BaseModal.vue'
import FormField from '../ui/FormField.vue'
import AlertBox from '../ui/AlertBox.vue'
import { proveedoresApi } from '../../api/resources'
import { catalogs } from '../../api/catalogs'
import { useEntityForm } from '../../composables/useEntityForm'
import { LIMITS, rules } from '../../utils/validation'

const props = defineProps({ proveedor: { type: Object, default: null } })
const emit = defineEmits(['close', 'saved'])

const L = LIMITS.proveedor
const formId = useId()

const { form, errors, saving, serverError, submit } = useEntityForm({
    initial: {
        nombre: props.proveedor?.nombre ?? '',
        ruc: props.proveedor?.ruc ?? '',
        telefono: props.proveedor?.telefono ?? '',
        correo: props.proveedor?.correo ?? '',
        direccion: props.proveedor?.direccion ?? '',
        activo: props.proveedor?.activo ?? true,
    },
    schema: {
        nombre: [rules.required(), rules.maxLength(L.nombre)],
        ruc: [rules.maxLength(L.ruc), rules.ruc()],
        telefono: [rules.maxLength(L.telefono), rules.phone()],
        correo: [rules.maxLength(L.correo), rules.email()],
        direccion: [rules.maxLength(L.direccion)],
    },
    save: data => (props.proveedor ? proveedoresApi.update(props.proveedor.id, data) : proveedoresApi.create(data)),
    onSaved: saved => {
        catalogs.proveedores.invalidate()
        emit('saved', saved)
    },
})
</script>
