export const money = (v, locale = 'es-PY', currency = 'PYG') =>
new Intl.NumberFormat(locale, { style: 'currency', currency }).format(v)