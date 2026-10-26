// URL base del API Gateway. En desarrollo es http://localhost:8080 (.env); en el build para
// los Tomcats es "/api", que Apache reenvía al gateway (.env.production).
export const API_BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '')

export const APP_NAME = 'Ferretería'
