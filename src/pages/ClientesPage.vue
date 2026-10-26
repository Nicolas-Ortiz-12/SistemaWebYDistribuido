<template>
    <PageHeader title="Clientes" subtitle="Personas y empresas a las que se les factura" :icon="IconUsers">
        <button type="button" class="btn btn-primary" @click="openForm(null)">
            <IconPlus :size="18" /> Nuevo cliente
        </button>
    </PageHeader>

    <section class="card">
        <div class="card-header">
            <div class="toolbar">
                <label class="input-icon">
                    <IconSearch :size="18" />
                    <span class="sr-only">Buscar clientes</span>
                    <input v-model="q" class="input" type="search" placeholder="Buscar por nombre, RUC o correo"
                        :maxlength="LIMITS.busqueda" autocomplete="off" />
                </label>
            </div>
            <span class="muted">{{ formatNumber(total) }} clientes</span>
        </div>

        <AlertBox v-if="error" type="error" class="card-body">{{ error }}</AlertBox>
        <div v-else-if="loading && !rows.length" class="loading-row">
            <IconLoader2 :size="20" class="spin" /> Cargando clientes…
        </div>
        <EmptyState v-else-if="!rows.length" :icon="IconUsers"
            :title="q ? 'Sin resultados' : 'Todavía no hay clientes'"
            :text="q ? 'Probá con otro texto de búsqueda.' : 'Registrá el primero con “Nuevo cliente”.'" />
        <div v-else class="table-wrap">
            <table class="table">
                <thead>
                    <tr>
                        <th>Cliente</th>
                        <th>RUC / CI</th>
                        <th>Contacto</th>
                        <th>Dirección</th>
                        <th class="col-actions"><span class="sr-only">Acciones</span></th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="c in rows" :key="c.id">
                        <td class="cell-main">{{ c.nombre }}</td>
                        <td class="mono">{{ c.ruc || '—' }}</td>
                        <td>
                            <div>{{ c.telefono || '—' }}</div>
                            <div v-if="c.correo" class="cell-sub">{{ c.correo }}</div>
                        </td>
                        <td>{{ c.direccion || '—' }}</td>
                        <td class="col-actions">
                            <div class="actions">
                                <button type="button" class="icon-btn" :aria-label="`Editar ${c.nombre}`" title="Editar"
                                    @click="openForm(c)">
                                    <IconPencil :size="18" />
                                </button>
                                <button type="button" class="icon-btn danger" :aria-label="`Eliminar ${c.nombre}`"
                                    title="Eliminar" @click="toDelete = c">
                                    <IconTrash :size="18" />
                                </button>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <ListPager v-if="total > 0" v-model:page="page" v-model:page-size="pageSize"
            :total="total" />
    </section>

    <ClienteForm v-if="formOpen" :cliente="editing" @close="formOpen = false" @saved="onSaved" />
    <ConfirmDialog v-if="toDelete" title="Eliminar cliente" :busy="deleting"
        :message="`¿Eliminar a “${toDelete.nombre}”? Esta acción no se puede deshacer.`" @cancel="toDelete = null"
        @confirm="confirmDelete" />
</template>

<script setup>
import { ref } from 'vue'
import { IconLoader2, IconPencil, IconPlus, IconSearch, IconTrash, IconUsers } from '@tabler/icons-vue'
import PageHeader from '../components/ui/PageHeader.vue'
import AlertBox from '../components/ui/AlertBox.vue'
import EmptyState from '../components/ui/EmptyState.vue'
import ListPager from '../components/ui/ListPager.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import ClienteForm from '../components/forms/ClienteForm.vue'
import { clientesApi } from '../api/resources'
import { catalogs } from '../api/catalogs'
import { usePagedList } from '../composables/usePagedList'
import { toast } from '../composables/useToast'
import { formatNumber } from '../utils/format'
import { LIMITS } from '../utils/validation'

const { q, page, pageSize, rows, total, loading, error, reload } = usePagedList(params => clientesApi.list(params))

const formOpen = ref(false)
const editing = ref(null)
const toDelete = ref(null)
const deleting = ref(false)

function openForm(cliente) {
    editing.value = cliente
    formOpen.value = true
}

function onSaved() {
    toast.success(editing.value ? 'Cliente actualizado.' : 'Cliente registrado.')
    formOpen.value = false
    reload()
}

async function confirmDelete() {
    deleting.value = true
    try {
        await clientesApi.remove(toDelete.value.id)
        catalogs.clientes.invalidate()
        toast.success('Cliente eliminado.')
        toDelete.value = null
        reload()
    } catch (e) {
        toast.error(e.message)
    } finally {
        deleting.value = false
    }
}
</script>
