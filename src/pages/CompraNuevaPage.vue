<template>
    <PageHeader title="Ingresar compra" subtitle="Cargá una factura de proveedor y sumá el stock" :icon="IconFileInvoice">
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
                <header class="card-header"><h2 class="card-title"><IconReceipt :size="18" /> Datos del comprobante</h2></header>
                <div class="card-body stack">
                    <FormField v-slot="f" label="Proveedor" required :error="errors.proveedor">
                        <SearchSelect v-model="header.proveedorId" :input-id="f.id" :selected-label="header.proveedorLabel"
                            :search="searchProveedores" placeholder="Buscar proveedor…" :invalid="f.invalid"
                            @select="opt => (header.proveedorLabel = opt?.label ?? '')" />
                    </FormField>
                    <FormField v-slot="f" label="Número de comprobante" required :error="errors.numero">
                        <input :id="f.id" :value="header.numero" class="input mono" :class="{ invalid: f.invalid }"
                            :aria-describedby="f.describedby" inputmode="numeric" maxlength="15" autocomplete="off"
                            placeholder="001-001-0000000" @input="onNumero" @blur="header.numero = padComprobante(header.numero)" />
                    </FormField>
                    <FormField v-slot="f" label="Fecha de emisión" required :error="errors.fecha">
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
                        <span class="total-highlight-label">Total de la compra</span>
                        <span class="total-highlight-value">{{ formatMoney(doc.totals.value.total) }}</span>
                    </div>
                    <AlertBox v-if="errors.lines" type="error">{{ errors.lines }}</AlertBox>
                    <AlertBox v-if="serverError" type="error">{{ serverError }}</AlertBox>
                    <button type="button" class="btn btn-primary btn-lg" :disabled="saving" @click="confirm">
                        <IconLoader2 v-if="saving" :size="20" class="spin" />
                        <IconCheck v-else :size="20" />
                        Confirmar compra
                    </button>
                    <button type="button" class="btn" :disabled="saving" @click="saveDraft">
                        <IconDeviceFloppy :size="18" /> Guardar borrador
                    </button>
                </div>
            </section>
        </aside>

        <section class="card sale-items">
            <header class="card-header">
                <h2 class="card-title"><IconTruckDelivery :size="18" /> Productos</h2>
            </header>
            <DocumentLines :doc="doc" price-editable />
        </section>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
    IconCheck, IconCoin, IconDeviceFloppy, IconFileInvoice, IconLoader2, IconPackage, IconReceipt, IconTruckDelivery,
} from '@tabler/icons-vue'
import PageHeader from '../components/ui/PageHeader.vue'
import AlertBox from '../components/ui/AlertBox.vue'
import FormField from '../components/ui/FormField.vue'
import SearchSelect from '../components/ui/SearchSelect.vue'
import DocumentLines from '../components/DocumentLines.vue'
import { comprasApi } from '../api/resources'
import { catalogs } from '../api/catalogs'
import { searchProveedores } from '../api/searchers'
import { useDocumentLines } from '../composables/useDocumentLines'
import { useDraft } from '../composables/useDraft'
import { toast } from '../composables/useToast'
import { parseDateInput, toDateInput } from '../utils/dates'
import { formatDateTime, formatMoney } from '../utils/format'
import { isComprobante, LIMITS, maskComprobante, padComprobante } from '../utils/validation'

const today = toDateInput()
const header = reactive({ proveedorId: null, proveedorLabel: '', numero: '', fecha: today })
const doc = useDocumentLines({ positivePrice: true })
const errors = ref({})
const serverError = ref('')
const saving = ref(false)

const drafts = useDraft('compra')
const draft = ref(drafts.load())

// Solo dígitos: los guiones se ponen solos mientras se escribe
function onNumero(event) {
    header.numero = maskComprobante(event.target.value)
    event.target.value = header.numero
}

function validateHeader() {
    const e = {}
    if (header.proveedorId == null) e.proveedor = 'Elegí el proveedor.'
    if (!header.numero) e.numero = 'Ingresá el número de comprobante.'
    else if (!isComprobante(header.numero)) e.numero = 'El formato es 001-001-0000456 (3, 3 y 7 dígitos).'
    if (!parseDateInput(header.fecha)) e.fecha = 'Ingresá una fecha válida.'
    else if (header.fecha > today) e.fecha = 'La fecha no puede ser futura.'
    return e
}

async function confirm() {
    serverError.value = ''
    errors.value = validateHeader()
    const linesError = doc.validateLines()
    if (linesError) errors.value.lines = linesError
    if (Object.keys(errors.value).length) return

    saving.value = true
    try {
        const compra = await comprasApi.create({
            proveedorId: header.proveedorId,
            numero: header.numero,
            // LocalDate en el backend: se manda el día tal cual, sin hora ni zona
            fechaEmision: header.fecha,
            detalles: doc.lines.value.map(l => ({
                productoId: l.productoId,
                cantidad: Number(l.cantidad),
                costoUnitario: Number(l.precio),
                tasaIva: Number(l.tasaIva),
            })),
        })
        toast.success(`Compra ${compra?.numero ?? header.numero} registrada por ${formatMoney(compra?.total ?? doc.totals.value.total)}.`)
        catalogs.productos.invalidate()
        drafts.clear()
        draft.value = null
        reset()
    } catch (e) {
        serverError.value = e.message
    } finally {
        saving.value = false
    }
}

function reset() {
    Object.assign(header, { proveedorId: null, proveedorLabel: '', numero: '', fecha: toDateInput() })
    doc.reset()
    errors.value = {}
}

function saveDraft() {
    const ok = drafts.save({ header: { ...header }, lines: doc.toDraft() })
    if (ok) toast.success('Borrador guardado en este equipo.')
    else toast.error('No se pudo guardar el borrador (almacenamiento del navegador bloqueado).')
    draft.value = null
}

function restoreDraft() {
    const { header: h, lines } = draft.value.data
    const proveedorId = Number(h?.proveedorId)
    header.proveedorId = Number.isInteger(proveedorId) && proveedorId > 0 ? proveedorId : null
    header.proveedorLabel = header.proveedorId ? String(h.proveedorLabel ?? '').slice(0, LIMITS.proveedor.nombre) : ''
    header.numero = maskComprobante(h?.numero)
    header.fecha = parseDateInput(h?.fecha) && h.fecha <= today ? h.fecha : today
    doc.fromDraft(lines)
    draft.value = null
}

function discardDraft() {
    drafts.clear()
    draft.value = null
}
</script>
