// Validación de formularios. Es una ayuda para el usuario: el backend vuelve a validar todo.

// Largos máximos de las columnas (ver ferreteria-api.yaml y UserAccount en Seguridad)
export const LIMITS = {
    cliente: { nombre: 75, ruc: 50, telefono: 50, correo: 200, direccion: 200 },
    proveedor: { nombre: 200, ruc: 50, telefono: 50, correo: 200, direccion: 200 },
    producto: { codigo: 64, nombre: 200, descripcion: 250, unidadMedida: 20 },
    categoria: { nombre: 120 },
    documento: { numero: 40 },
    // BCrypt solo usa los primeros 72 bytes de la contraseña
    login: { username: 80, password: 72 },
    busqueda: 100,
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
const PHONE_RE = /^[0-9+()\s-]{6,20}$/
const RUC_RE = /^[0-9][0-9.-]{2,19}$/
const CODE_RE = /^[\w.\-/]+$/

// Quita caracteres de control (saltos de línea, tabs, NUL...) que no van en un campo de una línea
export function cleanText(value) {
    // eslint-disable-next-line no-control-regex
    return String(value ?? '').replace(/[\u0000-\u001F\u007F]/g, '').trim()
}

// Copia del objeto con los textos limpios y los vacíos como null (el backend los guarda como NULL)
export function cleanForm(values) {
    return Object.fromEntries(Object.entries(values).map(([k, v]) => {
        if (typeof v !== 'string') return [k, v]
        const s = cleanText(v)
        return [k, s === '' ? null : s]
    }))
}

const isEmpty = v => v === null || v === undefined || String(v).trim() === ''

export const rules = {
    required: (msg = 'Este campo es obligatorio.') => v => (isEmpty(v) ? msg : null),
    maxLength: n => v => (!isEmpty(v) && String(v).trim().length > n ? `Máximo ${n} caracteres.` : null),
    email: () => v => (!isEmpty(v) && !EMAIL_RE.test(String(v).trim()) ? 'Correo electrónico inválido.' : null),
    phone: () => v => (!isEmpty(v) && !PHONE_RE.test(String(v).trim()) ? 'Teléfono inválido (solo números, espacios, +, - y paréntesis).' : null),
    ruc: () => v => (!isEmpty(v) && !RUC_RE.test(String(v).trim()) ? 'RUC inválido (solo números, puntos y guion).' : null),
    code: () => v => (!isEmpty(v) && !CODE_RE.test(String(v).trim()) ? 'Solo letras, números y . - _ /' : null),
    number: ({ min = 0, max = 1e12, integer = false, positive = false } = {}) => v => {
        if (isEmpty(v)) return null
        const n = Number(v)
        if (!Number.isFinite(n)) return 'Ingresá un número válido.'
        if (positive && n <= 0) return 'Debe ser mayor que cero.'
        if (n < min) return `Debe ser mayor o igual a ${min}.`
        if (n > max) return 'El número es demasiado grande.'
        if (integer && !Number.isInteger(n)) return 'Debe ser un número entero.'
        return null
    },
}

// schema: { campo: [regla, ...] } -> { campo: 'mensaje' } con el primer error de cada campo
export function validate(values, schema) {
    const errors = {}
    for (const [field, checks] of Object.entries(schema)) {
        for (const check of checks) {
            const error = check(values[field])
            if (error) {
                errors[field] = error
                break
            }
        }
    }
    return errors
}

// Número de comprobante con el formato de la factura legal: 001-001-0000456 (3-3-7 dígitos).
// Mientras se escribe se dejan solo los dígitos y se insertan los guiones solos.
const COMPROBANTE_RE = /^\d{3}-\d{3}-\d{7}$/

export function maskComprobante(value) {
    const digits = String(value ?? '').replace(/\D/g, '').slice(0, 13)
    return [digits.slice(0, 3), digits.slice(3, 6), digits.slice(6)].filter(Boolean).join('-')
}

// Al salir del campo, el último bloque se completa con ceros: 001-001-456 -> 001-001-0000456
export function padComprobante(value) {
    const [a = '', b = '', c = ''] = String(value ?? '').split('-')
    return a.length === 3 && b.length === 3 && c ? `${a}-${b}-${c.padStart(7, '0')}` : value
}

export const isComprobante = value => COMPROBANTE_RE.test(String(value ?? ''))

// Ruta interna segura para volver después del login: evita "//otro-sitio" y "/\otro-sitio"
export function safeRedirect(value) {
    return typeof value === 'string' && /^\/(?![/\\])[^\s\\]*$/.test(value) && !value.startsWith('/login')
        ? value
        : '/'
}
