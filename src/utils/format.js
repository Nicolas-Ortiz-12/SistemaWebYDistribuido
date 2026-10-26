import { parseDateInput } from './dates'

const LOCALE = 'es-PY'

const moneyFormat = new Intl.NumberFormat(LOCALE, { style: 'currency', currency: 'PYG', maximumFractionDigits: 0 })
const dateFormat = new Intl.DateTimeFormat(LOCALE, { day: '2-digit', month: '2-digit', year: 'numeric' })
const dateTimeFormat = new Intl.DateTimeFormat(LOCALE, { dateStyle: 'short', timeStyle: 'short' })
const shortDayFormat = new Intl.DateTimeFormat(LOCALE, { day: '2-digit', month: 'short' })
const longDateFormat = new Intl.DateTimeFormat(LOCALE, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

const toNumber = v => (Number.isFinite(Number(v)) ? Number(v) : 0)

// El número de factura de una venta se genera solo a partir de su id: 001-001-0000024
export const formatInvoiceNumber = id => `001-001-${String(Number(id) || 0).padStart(7, '0')}`

export const formatMoney = v => moneyFormat.format(toNumber(v))

export const formatNumber = (v, maxDecimals = 2) =>
    new Intl.NumberFormat(LOCALE, { maximumFractionDigits: maxDecimals }).format(toNumber(v))

// Acepta un Date, un instante ISO o una fecha "yyyy-MM-dd" (que se toma como día local)
function toDate(value) {
    if (value instanceof Date) return value
    if (typeof value !== 'string' || !value) return null
    const d = parseDateInput(value) ?? new Date(value)
    return Number.isNaN(d.getTime()) ? null : d
}

export const formatDate = v => { const d = toDate(v); return d ? dateFormat.format(d) : '—' }
export const formatDateTime = v => { const d = toDate(v); return d ? dateTimeFormat.format(d) : '—' }
export const formatShortDay = v => { const d = toDate(v); return d ? shortDayFormat.format(d) : '—' }
export const formatLongDate = v => { const d = toDate(v); return d ? longDateFormat.format(d) : '—' }
