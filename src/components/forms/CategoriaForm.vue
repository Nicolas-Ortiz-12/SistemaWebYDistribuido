<template>
    <BaseModal :title="categoria ? 'Editar categoría' : 'Nueva categoría'" :icon="IconCategory" size="sm" :busy="saving"
        @close="emit('close')">
        <form :id="formId" class="stack" novalidate @submit.prevent="submit">
            <FormField v-slot="f" label="Nombre" required :error="errors.nombre">
                <input :id="f.id" v-model="form.nombre" class="input" :class="{ invalid: f.invalid }"
                    :aria-describedby="f.describedby" :maxlength="LIMITS.categoria.nombre" autocomplete="off" />
            </FormField>
            <AlertBox v-if="serverError" type="error">{{ serverError }}</AlertBox>
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
import { IconCategory, IconDeviceFloppy, IconLoader2 } from '@tabler/icons-vue'
import BaseModal from '../ui/BaseModal.vue'
import FormField from '../ui/FormField.vue'
import AlertBox from '../ui/AlertBox.vue'
import { categoriasApi } from '../../api/resources'
import { catalogs } from '../../api/catalogs'
import { useEntityForm } from '../../composables/useEntityForm'
import { LIMITS, rules } from '../../utils/validation'

const props = defineProps({ categoria: { type: Object, default: null } })
const emit = defineEmits(['close', 'saved'])

const formId = useId()

const { form, errors, saving, serverError, submit } = useEntityForm({
    initial: { nombre: props.categoria?.nombre ?? '' },
    schema: { nombre: [rules.required(), rules.maxLength(LIMITS.categoria.nombre)] },
    save: data => (props.categoria ? categoriasApi.update(props.categoria.id, data) : categoriasApi.create(data)),
    onSaved: saved => {
        catalogs.categorias.invalidate()
        emit('saved', saved)
    },
})
</script>
