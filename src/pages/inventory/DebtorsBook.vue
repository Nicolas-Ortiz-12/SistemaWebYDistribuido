<template>
    <section class="card">
        <div class="card-header">
            <div>
                <h2 class="title">Libro de Deudores</h2>
                <p class="page-subtitle">Gestión de cuentas por cobrar (Fiado).</p>
            </div>
        </div>

        <div class="card-body">
            <div v-if="loading" class="loading">Cargando deudores...</div>
            <div v-if="error" class="error">{{ error }}</div>

            <div v-if="!loading && !error && debtors.length === 0" class="empty-state" style="text-align: center; padding: 3rem; color: #666;">
                <p>No hay deudores con saldo pendiente.</p>
            </div>

            <div class="grid-cards">
                <div v-for="d in debtors" :key="d.id" class="debtor-card">
                    <div class="debtor-info">
                        <h3 class="debtor-name">{{ d.nombre }}</h3>
                        <p class="debtor-debt">Deuda: <strong>{{ money(d.totalAdeudado) }}</strong></p>
                    </div>
                    <div class="debtor-actions">
                        <button class="btn btn-primary btn-sm" @click="clearDebt(d.id)" :disabled="clearingId === d.id">
                            <span v-if="clearingId === d.id">Saldando...</span>
                            <span v-else>Saldar Deuda</span>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchWithAuth } from '../../services/authService'
import { API_BASE } from '../../services/api'

const debtors = ref([])
const loading = ref(false)
const error = ref('')
const clearingId = ref(null)

const money = (v) => new Intl.NumberFormat('es-PY', { style: 'currency', currency: 'PYG' }).format(v)

async function fetchDebtors() {
    loading.value = true
    error.value = ''
    try {
        const res = await fetchWithAuth(`${API_BASE}/deudores/con-deuda/0/100`)
        debtors.value = Array.isArray(res?.content) ? res.content : []
    } catch (e) {
        error.value = 'No se pudieron cargar los deudores.'
        console.error(e)
    } finally {
        loading.value = false
    }
}

async function clearDebt(id) {
    if (!confirm('¿Confirmas que el deudor ha saldado su deuda en su totalidad?')) return
    
    clearingId.value = id
    try {
        await fetchWithAuth(`${API_BASE}/deudores/${id}/saldar`, {
            method: 'PUT'
        })
        await fetchDebtors() // Recargar lista
    } catch (e) {
        alert('No se pudo saldar la deuda.')
        console.error(e)
    } finally {
        clearingId.value = null
    }
}

onMounted(() => {
    fetchDebtors()
})
</script>

<style scoped>
.grid-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1.5rem;
    margin-top: 1rem;
}
.debtor-card {
    background: var(--bg-card, #fff);
    border: 1px solid var(--border-color, #e2e8f0);
    border-radius: 12px;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
    transition: transform 0.2s, box-shadow 0.2s;
}
.debtor-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1);
}
.debtor-name {
    font-size: 1.2rem;
    font-weight: 600;
    margin-top: 0;
    margin-bottom: 0.5rem;
}
.debtor-debt {
    font-size: 1rem;
    color: #ef4444; /* Rojo para la deuda */
    margin-bottom: 1.5rem;
}
.debtor-actions {
    text-align: right;
}
</style>
