// Borradores de formularios largos (ventas, compras) guardados en este equipo, por usuario.
import { currentUser } from '../auth/session'

export function useDraft(name) {
    const key = () => `sw.draft.${currentUser.value?.username ?? 'anon'}.${name}`

    function save(data) {
        try {
            localStorage.setItem(key(), JSON.stringify({ savedAt: Date.now(), data }))
            return true
        } catch {
            return false
        }
    }

    // Devuelve { savedAt, data } o null; el contenido lo valida quien lo restaura
    function load() {
        try {
            const parsed = JSON.parse(localStorage.getItem(key()) || 'null')
            return parsed && typeof parsed === 'object' && parsed.data && typeof parsed.data === 'object' ? parsed : null
        } catch {
            return null
        }
    }

    function clear() {
        try {
            localStorage.removeItem(key())
        } catch { /* nada que borrar */ }
    }

    return { save, load, clear }
}
