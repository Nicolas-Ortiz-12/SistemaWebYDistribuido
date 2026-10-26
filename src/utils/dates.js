// Fechas en hora local. toISOString() trabaja en UTC: en Paraguay (UTC-3), después de las 21:00
// ya devuelve el día siguiente, por eso todas las conversiones pasan por acá.
const pad = n => String(n).padStart(2, '0')
const DATE_RE = /^(\d{4})-(\d{2})-(\d{2})$/

// Date -> "yyyy-MM-dd" (formato de <input type="date"> y de LocalDate en el backend)
export function toDateInput(date = new Date()) {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

// "yyyy-MM-dd" -> Date a las 00:00 locales (new Date("yyyy-MM-dd") lo interpreta en UTC)
export function parseDateInput(value) {
    const m = DATE_RE.exec(value ?? '')
    if (!m) return null
    const date = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
    return toDateInput(date) === value ? date : null
}

// Date -> ISO-8601 con el offset local, p. ej. "2026-10-20T14:30:00-03:00"
export function toOffsetIso(date) {
    const offset = -date.getTimezoneOffset()
    const sign = offset >= 0 ? '+' : '-'
    const abs = Math.abs(offset)
    return `${toDateInput(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}` +
        `${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`
}

export function startOfDayIso(value) {
    const d = parseDateInput(value)
    return d ? toOffsetIso(d) : undefined
}

export function endOfDayIso(value) {
    const d = parseDateInput(value)
    if (!d) return undefined
    d.setHours(23, 59, 59)
    return toOffsetIso(d)
}

export function addDays(date, days) {
    const d = new Date(date)
    d.setDate(d.getDate() + days)
    return d
}

export function daysBetween(fromValue, toValue) {
    const from = parseDateInput(fromValue)
    const to = parseDateInput(toValue)
    if (!from || !to) return NaN
    return Math.round((to - from) / 86_400_000)
}

// Lista de "yyyy-MM-dd" desde fromValue hasta toValue inclusive
export function dayRange(fromValue, toValue) {
    const days = []
    const end = parseDateInput(toValue)
    for (let d = parseDateInput(fromValue); d && end && d <= end; d = addDays(d, 1)) days.push(toDateInput(d))
    return days
}

// Clave de día local de un instante del backend (OffsetDateTime en UTC)
export function localDayKey(isoInstant) {
    const d = new Date(isoInstant)
    return Number.isNaN(d.getTime()) ? null : toDateInput(d)
}
