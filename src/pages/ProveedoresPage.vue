<template>
    <PageHeader title="Proveedores" subtitle="Empresas a las que se les compra mercadería" :icon="IconTruck">
        <button v-if="isAdmin" type="button" class="btn btn-primary" @click="openForm(null)">
            <IconPlus :size="18" /> Nuevo proveedor
        </button>
    </PageHeader>

    <AlertBox v-if="!isAdmin" type="info">Solo un administrador puede crear, editar o eliminar proveedores.</AlertBox>

    <section class="card">
        <div class="card-header">
            <div class="toolbar">
                <label class="input-icon">
                    <IconSearch :size="18" />
                    <span class="sr-only">Buscar proveedores</span>
                    <input v-model="q" class="input" type="search" placeholder="Buscar por nombre o RUC"
                        :maxlength="LIMITS.busqueda" autocomplete="off" />
                </label>
            </div>
            <span class="muted">{{ formatNumber(total) }} proveedores</span>
        </div>

        <AlertBox v-if="error" type="error" class="card-body">{{ error }}</AlertBox>
        <div v-else-if="loading && !rows.length" class="loading-row">
            <IconLoader2 :size="20" class="spin" /> Cargando proveedores…
        </div>
        <EmptyState v-else-if="!rows.length" :icon="IconTruck" :title="q ? 'Sin resultados' : 'Todavía no hay proveedores'"
            :text="q ? 'Probá con otro texto de búsqueda.' : ''" />
        <div v-else class="table-wrap">
            <table class="table">
                <thead>
                    <tr>
                        <th>Proveedor</th>
                        <th>RUC</th>
                        <th>Contacto</th>
                        <th>Dirección</th>
                        <th>Estado</th>
                        <th v-if="isAdmin" class="col-actions"><span class="sr-only">Acciones</span></th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="p in rows" :key="p.id">
                        <td class="cell-main">{{ p.nombre }}</td>
                        <td class="mono">{{ p.ruc || '—' }}</td>
                        <td>
                            <div>{{ p.telefono || '—' }}</div>
                            <div v-if="p.correo" class="cell-sub">{{ p.correo }}</div>
                        </td>
                        <td>{{ p.direccion || '—' }}</td>
                        <td>
                            <span class="badge" :class="p.activo === false ? '' : 'badge-success'">
                                {{ p.activo === false ? 'Inactivo' : 'Activo' }}
                            </span>
                        </td>
                        <td v-if="isAdmin" class="col-actions">
                            <div class="actions">
                                <button type="button" class="icon-btn" :aria-label="`Editar ${p.nombre}`" title="Editar"
                                    @click="openForm(p)">
                                    <IconPencil :size="18" />
                                </button>
                                <button type="button" class="icon-btn danger" :aria-label="`Eliminar ${p.nombre}`"
                                    title="Eliminar" @click="toDelete = p">
                                    <IconTrash :size="18" />
                                </button>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <ListPager v-if="total > 0" v-model:page="page" v-model:page-size="pageSize" :total="total" />
    </section>

    <ProveedorForm v-if="formOpen" :proveedor="editing" @close="formOpen = false" @saved="onSaved" />
    <ConfirmDialog v-if="toDelete" title="Eliminar proveedor" :busy="deleting"
        :message="`¿Eliminar a “${toDelete.nombre}”? Esta acción no se puede deshacer.`" @cancel="toDelete = null"
        @confirm="confirmDelete" />
</template>

<script setup>
import { computed, ref } from 'vue'
import { IconLoader2, IconPencil, IconPlus, IconSearch, IconTrash, IconTruck } from '@tabler/icons-vue'
import PageHeader from '../components/ui/PageHeader.vue'
import AlertBox from '../components/ui/AlertBox.vue'
import EmptyState from '../components/ui/EmptyState.vue'
import ListPager from '../components/ui/ListPager.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import ProveedorForm from '../components/forms/ProveedorForm.vue'
import { proveedoresApi } from '../api/resources'
import { catalogs } from '../api/catalogs'
import { hasRole } from '../auth/session'
import { usePagedList } from '../composables/usePagedList'
import { toast } from '../composables/useToast'
import { formatNumber } from '../utils/format'
import { LIMITS } from '../utils/validation'

const { q, page, pageSize, rows, total, loading, error, reload } = usePagedList(params => proveedoresApi.list(params))

// Alta, edición y baja de proveedores son solo de ADMIN (lo exige el backend)
const isAdmin = computed(() => hasRole('ADMIN'))

const formOpen = ref(false)
const editing = ref(null)
const toDelete = ref(null)
const deleting = ref(false)

function openForm(proveedor) {
    editing.value = proveedor
    formOpen.value = true
}

function onSaved() {
    toast.success(editing.value ? 'Proveedor actualizado.' : 'Proveedor registrado.')
    formOpen.value = false
    reload()
}

async function confirmDelete() {
    deleting.value = true
    try {
        await proveedoresApi.remove(toDelete.value.id)
        catalogs.proveedores.invalidate()
        toast.success('Proveedor eliminado.')
        toDelete.value = null
        reload()
    } catch (e) {
        toast.error(e.message)
    } finally {
        deleting.value = false
    }
}
</script>
