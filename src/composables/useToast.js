// Avisos breves (éxito / error) que reemplazan a alert(): no bloquean la página.
import { reactive } from 'vue'

const DURATION_MS = 4500
let nextId = 1

export const toasts = reactive([])

function push(type, message) {
    const id = nextId++
    toasts.push({ id, type, message: String(message) })
    setTimeout(() => dismissToast(id), DURATION_MS)
}

export function dismissToast(id) {
    const i = toasts.findIndex(t => t.id === id)
    if (i !== -1) toasts.splice(i, 1)
}

export const toast = {
    success: message => push('success', message),
    error: message => push('error', message),
}
