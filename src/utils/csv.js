// Exportación a CSV (separador ";" para Excel en español).

// Un texto que empieza con = + - @ se ejecuta como fórmula al abrir el archivo en Excel
// (inyección CSV): se le antepone un apóstrofo para que quede como texto.
function cell(value) {
    if (value === null || value === undefined) return ''
    if (typeof value === 'number') return Number.isFinite(value) ? String(value).replace('.', ',') : ''
    let s = String(value)
    if (/^[=+\-@\t\r]/.test(s)) s = `'${s}`
    return /[";\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

export function downloadCsv(filename, headers, rows) {
    const lines = [headers, ...rows].map(row => row.map(cell).join(';'))
    // El BOM hace que Excel lea el archivo como UTF-8 (tildes y ñ)
    const blob = new Blob(['﻿' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
}
