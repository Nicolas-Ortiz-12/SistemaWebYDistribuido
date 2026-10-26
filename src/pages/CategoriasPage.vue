<template>
    <PageHeader title="Categorías" subtitle="Agrupan los productos del catálogo" :icon="IconCategory">
        <button type="button" class="btn btn-primary" @click="openForm(null)">
            <IconPlus :size="18" /> Nueva categoría
        </button>
    </PageHeader>

    <section class="card">
        <div class="card-header">
            <div class="toolbar">
                <label class="input-icon">
                    <IconSearch :size="18" />
                    <span class="sr-only">Buscar categorías</span>
                    <input v-model="q" class="input" type="search" placeholder="Buscar por nombre"
                        :maxlength="LIMITS.busqueda" autocomplete="off" />
                </label>
            </div>
            <span class="muted">{{ formatNumber(total) }} categorías</span>
        </div>

        <AlertBox v-if="error" type="error" class="card-body">{{ error }}</AlertBox>
        <div v-else-if="loading && !rows.length" class="loading-row">
            <IconLoader2 :size="20" class="spin" /> Cargando categorías…
        </div>
        <EmptyState v-else-if="!rows.length" :icon="IconCategory"
            :title="q ? 'Sin resultados' : 'Todavía no hay categorías'" :text="q ? 'Probá con otro texto de búsqueda.' : ''" />
        <div v-else class="table-wrap">
            <table class="table">
                <thead>
                    <tr>
                        <th class="col-actions">#</th>
                        <th>Nombre</th>
                        <th class="col-actions"><span class="sr-only">Acciones</span></th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="c in rows" :key="c.id">
                        <td class="muted mono">{{ c.id }}</td>
                        <td class="cell-main">{{ c.nombre }}</td>
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

        <ListPager v-if="total > 0" v-model:page="page" v-model:page-size="pageSize" :total="total" />
    </section>

    <CategoriaForm v-if="formOpen" :categoria="editing" @close="formOpen = false" @saved="onSaved" />
    <ConfirmDialog v-if="toDelete" title="Eliminar categoría" :busy="deleting"
        :message="`¿Eliminar la categoría “${toDelete.nombre}”?`" @cancel="toDelete = null" @confirm="confirmDelete" />
</template>

<script setup>
import { ref } from 'vue'
import { IconCategory, IconLoader2, IconPencil, IconPlus, IconSearch, IconTrash } from '@tabler/icons-vue'
import PageHeader from '../components/ui/PageHeader.vue'
import AlertBox from '../components/ui/AlertBox.vue'
import EmptyState from '../components/ui/EmptyState.vue'
import ListPager from '../components/ui/ListPager.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import CategoriaForm from '../components/forms/CategoriaForm.vue'
import { categoriasApi } from '../api/resources'
import { catalogs } from '../api/catalogs'
import { usePagedList } from '../composables/usePagedList'
import { toast } from '../composables/useToast'
import { formatNumber } from '../utils/format'
import { LIMITS } from '../utils/validation'

const { q, page, pageSize, rows, total, loading, error, reload } = usePagedList(params => categoriasApi.list(params))

const formOpen = ref(false)
const editing = ref(null)
const toDelete = ref(null)
const deleting = ref(false)

function openForm(categoria) {
    editing.value = categoria
    formOpen.value = true
}

function onSaved() {
    toast.success(editing.value ? 'Categoría actualizada.' : 'Categoría creada.')
    formOpen.value = false
    reload()
}

async function confirmDelete() {
    deleting.value = true
    try {
        await categoriasApi.remove(toDelete.value.id)
        catalogs.categorias.invalidate()
        toast.success('Categoría eliminada.')
        toDelete.value = null
        reload()
    } catch (e) {
        toast.error(e.message)
    } finally {
        deleting.value = false
    }
}
</script>
