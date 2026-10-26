<template>
    <BaseModal :title="cliente ? 'Editar cliente' : 'Nuevo cliente'" :icon="IconUsers" :busy="saving" @close="emit('close')">
        <form :id="formId" class="form-grid" novalidate @submit.prevent="submit">
            <FormField v-slot="f" label="Nombre o razón social" required :error="errors.nombre" class="span-all">
                <input :id="f.id" v-model="form.nombre" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.nombre" autocomplete="off" />
            </FormField>
            <FormField v-slot="f" label="RUC / CI" :error="errors.ruc">
                <input :id="f.id" v-model="form.ruc" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="L.ruc" inputmode="numeric" autocomplete="off"
                    placeholder="1234567-8" />
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
import { IconDeviceFloppy, IconLoader2, IconUsers } from '@tabler/icons-vue'
import BaseModal from '../ui/BaseModal.vue'
import FormField from '../ui/FormField.vue'
import AlertBox from '../ui/AlertBox.vue'
import { clientesApi } from '../../api/resources'
import { catalogs } from '../../api/catalogs'
import { useEntityForm } from '../../composables/useEntityForm'
import { LIMITS, rules } from '../../utils/validation'

const props = defineProps({ cliente: { type: Object, default: null } })
const emit = defineEmits(['close', 'saved'])

const L = LIMITS.cliente
const formId = useId()

const { form, errors, saving, serverError, submit } = useEntityForm({
    initial: {
        nombre: props.cliente?.nombre ?? '',
        ruc: props.cliente?.ruc ?? '',
        telefono: props.cliente?.telefono ?? '',
        correo: props.cliente?.correo ?? '',
        direccion: props.cliente?.direccion ?? '',
    },
    schema: {
        nombre: [rules.required(), rules.maxLength(L.nombre)],
        ruc: [rules.maxLength(L.ruc), rules.ruc()],
        telefono: [rules.maxLength(L.telefono), rules.phone()],
        correo: [rules.maxLength(L.correo), rules.email()],
        direccion: [rules.maxLength(L.direccion)],
    },
    save: data => (props.cliente ? clientesApi.update(props.cliente.id, data) : clientesApi.create(data)),
    onSaved: saved => {
        catalogs.clientes.invalidate()
        emit('saved', saved)
    },
})
</script>
