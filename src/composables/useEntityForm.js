// Lógica común de los formularios de alta/edición: validar, limpiar los textos y guardar.
import { reactive, ref } from 'vue'
import { cleanForm, validate } from '../utils/validation'

export function useEntityForm({ initial, schema, save, onSaved }) {
    const form = reactive({ ...initial })
    const errors = ref({})
    const saving = ref(false)
    const serverError = ref('')

    async function submit() {
        errors.value = validate(form, schema)
        if (Object.keys(errors.value).length > 0) return
        saving.value = true
        serverError.value = ''
        try {
            onSaved(await save(cleanForm(form)))
        } catch (e) {
            serverError.value = e.message
        } finally {
            saving.value = false
        }
    }

    return { form, errors, saving, serverError, submit }
}
