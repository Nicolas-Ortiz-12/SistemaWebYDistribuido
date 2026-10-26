<template>
    <PageHeader title="Nueva venta" subtitle="Registrá una venta y descontá el stock" :icon="IconCashRegister">
        <RouterLink class="btn" to="/productos/lista"><IconPackage :size="18" /> Ver productos</RouterLink>
    </PageHeader>

    <AlertBox v-if="draft" type="info">
        <div class="row-between">
            <span>Tenés un borrador guardado el {{ formatDateTime(new Date(draft.savedAt)) }}.</span>
            <span class="row">
                <button type="button" class="btn btn-sm" @click="restoreDraft">Restaurar</button>
                <button type="button" class="btn btn-sm btn-ghost" @click="discardDraft">Descartar</button>
            </span>
        </div>
    </AlertBox>

    <div class="sale-layout">
        <aside class="sale-side">
            <section class="card">
                <header class="card-header"><h2 class="card-title"><IconReceipt :size="18" /> Datos de la venta</h2></header>
                <div class="card-body stack">
                    <FormField v-slot="f" label="Cliente" hint="Dejalo vacío para “Consumidor final”.">
                        <SearchSelect v-model="header.clienteId" :input-id="f.id" :selected-label="header.clienteLabel"
                            :search="searchClientes" placeholder="Buscar cliente…"
                            @select="opt => (header.clienteLabel = opt?.label ?? '')" />
                    </FormField>
                    <FormField v-slot="f" label="Número de factura">
                        <input :id="f.id" class="input mono" :value="numero" readonly aria-readonly="true" />
                    </FormField>
                    <FormField v-slot="f" label="Fecha" required :error="errors.fecha">
                        <input :id="f.id" v-model="header.fecha" class="input" :class="{ invalid: f.invalid }" type="date"
                            :max="today" />
                    </FormField>
                </div>
            </section>

            <section class="card sale-summary">
                <header class="card-header"><h2 class="card-title"><IconCoin :size="18" /> Montos</h2></header>
                <div class="card-body stack">
                    <div class="totals">
                        <div class="totals-row"><span>Subtotal</span><span>{{ formatMoney(doc.totals.value.subtotal) }}</span></div>
                        <div class="totals-row"><span>IVA</span><span>{{ formatMoney(doc.totals.value.iva) }}</span></div>
                    </div>
                    <div class="total-highlight" aria-live="polite">
                        <span class="total-highlight-label">Total a cobrar</span>
                        <span class="total-highlight-value">{{ formatMoney(doc.totals.value.total) }}</span>
                    </div>
                    <AlertBox v-if="errors.lines" type="error">{{ errors.lines }}</AlertBox>
                    <AlertBox v-if="serverError" type="error">{{ serverError }}</AlertBox>
                    <button type="button" class="btn btn-primary btn-lg" :disabled="saving" @click="confirm">
                        <IconLoader2 v-if="saving" :size="20" class="spin" />
                        <IconCheck v-else :size="20" />
                        Confirmar venta
                    </button>
                    <button type="button" class="btn" :disabled="saving" @click="saveDraft">
                        <IconDeviceFloppy :size="18" /> Guardar borrador
                    </button>
                </div>
            </section>
        </aside>

        <section class="card sale-items">
            <header class="card-header">
                <h2 class="card-title"><IconShoppingCart :size="18" /> Productos</h2>
            </header>
            <DocumentLines :doc="doc" check-stock />
        </section>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
    IconCashRegister, IconCheck, IconCoin, IconDeviceFloppy, IconLoader2, IconPackage, IconReceipt, IconShoppingCart,
} from '@tabler/icons-vue'
import PageHeader from '../components/ui/PageHeader.vue'
import AlertBox from '../components/ui/AlertBox.vue'
import FormField from '../components/ui/FormField.vue'
import SearchSelect from '../components/ui/SearchSelect.vue'
import DocumentLines from '../components/DocumentLines.vue'
import { productosApi, ventasApi } from '../api/resources'
import { catalogs } from '../api/catalogs'
import { productoLabel, searchClientes } from '../api/searchers'
import { useDocumentLines } from '../composables/useDocumentLines'
import { useDraft } from '../composables/useDraft'
import { toast } from '../composables/useToast'
import { parseDateInput, toDateInput, toOffsetIso } from '../utils/dates'
import { formatDateTime, formatInvoiceNumber, formatMoney } from '../utils/format'
import { LIMITS } from '../utils/validation'

const today = toDateInput()
const header = reactive({ clienteId: null, clienteLabel: '', fecha: today })
const doc = useDocumentLines({ checkStock: true })
// Número de la próxima venta: se muestra, pero lo asigna el sistema
const numero = ref('Autogenerado')
const errors = ref({})
const serverError = ref('')
const saving = ref(false)

const drafts = useDraft('venta')
const draft = ref(drafts.load())

function validateHeader() {
    const e = {}
    const fecha = parseDateInput(header.fecha)
    if (!fecha) e.fecha = 'Ingresá una fecha válida.'
    else if (header.fecha > today) e.fecha = 'La fecha no puede ser futura.'
    return e
}

// La venta de hoy lleva la hora actual; una de otro día se registra al mediodía de ese día
function fechaVenta() {
    if (header.fecha === today) return toOffsetIso(new Date())
    const d = parseDateInput(header.fecha)
    d.setHours(12, 0, 0)
    return toOffsetIso(d)
}

async function confirm() {
    serverError.value = ''
    errors.value = validateHeader()
    const linesError = doc.validateLines()
    if (linesError) errors.value.lines = linesError
    if (Object.keys(errors.value).length) return

    saving.value = true
    try {
        const venta = await ventasApi.create({
            clienteId: header.clienteId,
            fechaVenta: fechaVenta(),
            // El precio no se envía: el backend cobra el precio de lista vigente
            items: doc.lines.value.map(l => ({ productoId: l.productoId, cantidad: Number(l.cantidad), tasaIva: Number(l.tasaIva) })),
        })
        toast.success(`Venta ${venta?.id ? formatInvoiceNumber(venta.id) : ''} registrada por ${formatMoney(venta?.total ?? doc.totals.value.total)}.`)
        catalogs.productos.invalidate()
        drafts.clear()
        draft.value = null
        reset()
        loadNumero()
    } catch (e) {
        serverError.value = e.message
    } finally {
        saving.value = false
    }
}

function reset() {
    Object.assign(header, { clienteId: null, clienteLabel: '', fecha: toDateInput() })
    doc.reset()
    errors.value = {}
}

function saveDraft() {
    const ok = drafts.save({ header: { ...header }, lines: doc.toDraft() })
    if (ok) toast.success('Borrador guardado en este equipo.')
    else toast.error('No se pudo guardar el borrador (almacenamiento del navegador bloqueado).')
    draft.value = null
}

async function restoreDraft() {
    const { header: h, lines } = draft.value.data
    const clienteId = Number(h?.clienteId)
    header.clienteId = Number.isInteger(clienteId) && clienteId > 0 ? clienteId : null
    header.clienteLabel = header.clienteId ? String(h.clienteLabel ?? '').slice(0, LIMITS.cliente.nombre) : ''
    header.fecha = parseDateInput(h?.fecha) && h.fecha <= today ? h.fecha : today
    doc.fromDraft(lines)
    draft.value = null
    await refreshPrices()
}

// El precio y el stock pudieron cambiar desde que se guardó el borrador
async function refreshPrices() {
    const results = await Promise.allSettled(doc.lines.value.map(l => productosApi.get(l.productoId)))
    const missing = []
    results.forEach((r, i) => {
        const line = doc.lines.value[i]
        if (r.status === 'fulfilled' && r.value) {
            line.precio = Number(r.value.precio) || 0
            line.stock = Number(r.value.stockMinimo) || 0
            line.label = productoLabel(r.value)
        } else {
            missing.push(line.label || `#${line.productoId}`)
        }
    })
    if (missing.length) toast.error(`No se encontraron estos productos del borrador: ${missing.join(', ')}.`)
}

async function loadNumero() {
    try {
        numero.value = (await ventasApi.nextNumber())?.numero || 'Autogenerado'
    } catch {
        numero.value = 'Autogenerado'
    }
}

onMounted(loadNumero)

function discardDraft() {
    drafts.clear()
    draft.value = null
}
</script>
