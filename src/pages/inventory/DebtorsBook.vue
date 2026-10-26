<template>
    <section class="card">
        <div class="card-header">
            <div>
                <h2 class="title">Libro de Deudores</h2>
                <p class="page-subtitle">Gestion de cuentas por cobrar (Fiado).</p>
            </div>
        </div>

        <div class="card-body">
            <div v-if="loading" class="loading">Cargando deudores...</div>
            <div v-if="error" class="error">{{ error }}</div>

            <div v-if="!loading && !error && debtors.length === 0" class="empty-state">
                <p>No hay deudores con saldo pendiente.</p>
            </div>

            <div class="grid-cards">
                <article v-for="d in debtors" :key="d.id" class="debtor-card">
                    <div class="debtor-info">
                        <h3 class="debtor-name">{{ d.nombre }}</h3>
                        <p class="debtor-debt">Deuda actual</p>
                        <strong class="debtor-balance">{{ money(d.totalAdeudado) }}</strong>
                    </div>
                    <div class="debtor-actions">
                        <button
                            class="debtor-clear-btn"
                            @click="clearDebt(d.id)"
                            :disabled="clearingId === d.id"
                        >
                            <span v-if="clearingId === d.id">Saldando...</span>
                            <span v-else>Clear Debt</span>
                        </button>
                    </div>
                </article>
            </div>
        </div>
    </section>
</template>

<script setup>
import { ref, shallowRef, onMounted } from 'vue'
import { fetchWithAuth } from '../../services/authService'
import { API_BASE } from '../../services/api'

const debtors = ref([])
const loading = shallowRef(false)
const error = shallowRef('')
const clearingId = shallowRef(null)

const money = (v) => new Intl.NumberFormat('es-PY', { style: 'currency', currency: 'PYG' }).format(v)

async function fetchDebtors() {
    loading.value = true
    error.value = ''
    try {
        const res = await fetchWithAuth(`${API_BASE}/deudores/con-deuda/0/100`)
        debtors.value = Array.isArray(res?.content) ? res.content : []
    } catch (e) {
        error.value = e?.message || 'No se pudieron cargar los deudores.'
        console.error(e)
    } finally {
        loading.value = false
    }
}

async function clearDebt(id) {
    if (!confirm('Confirmas que el deudor ya saldó su deuda en su totalidad?')) return

    clearingId.value = id
    try {
        await fetchWithAuth(`${API_BASE}/deudores/${id}/saldar`, {
            method: 'PUT'
        })
        await fetchDebtors()
    } catch (e) {
        alert(e?.message || 'No se pudo saldar la deuda.')
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
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 1rem;
    margin-top: 1rem;
}

.debtor-card {
    background: var(--panel, #fff);
    border: 1px solid var(--border, #ddd7c8);
    border-radius: 14px;
    padding: 1rem;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 1rem;
    box-shadow: 0 6px 18px rgba(24, 20, 12, 0.06);
}

.debtor-name {
    margin: 0 0 0.35rem;
    font-size: 1.05rem;
    line-height: 1.2;
}

.debtor-debt {
    margin: 0;
    color: var(--muted, #6d6a62);
    font-size: 0.85rem;
}

.debtor-balance {
    display: block;
    margin-top: 0.35rem;
    font-size: 1.1rem;
    color: #0f7a3a;
}

.debtor-actions {
    display: flex;
    justify-content: flex-end;
}

.debtor-clear-btn {
    min-height: 42px;
    padding: 0 14px;
    border: 1px solid #1f8a46;
    border-radius: 12px;
    background: linear-gradient(180deg, #dff5e7 0%, #8ed9aa 100%);
    color: #155c2f;
    font-weight: 700;
}

.debtor-clear-btn:disabled {
    opacity: 0.7;
}

.empty-state {
    text-align: center;
    padding: 2rem 1rem;
    color: var(--muted, #6d6a62);
}
</style>
