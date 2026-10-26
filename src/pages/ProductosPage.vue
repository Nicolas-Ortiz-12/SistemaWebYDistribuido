<template>
    <PageHeader title="Productos" subtitle="Catálogo, precios y stock disponible" :icon="IconPackage">
        <button type="button" class="btn btn-primary" @click="openForm(null)">
            <IconPlus :size="18" /> Nuevo producto
        </button>
    </PageHeader>

    <section class="card">
        <div class="card-header">
            <div class="toolbar">
                <label class="input-icon">
                    <IconSearch :size="18" />
                    <span class="sr-only">Buscar productos</span>
                    <input v-model="q" class="input" type="search" placeholder="Buscar por nombre o descripción"
                        :maxlength="LIMITS.busqueda" autocomplete="off" />
                </label>
            </div>
            <span class="muted">{{ formatNumber(total) }} productos</span>
        </div>

        <AlertBox v-if="error" type="error" class="card-body">{{ error }}</AlertBox>
        <div v-else-if="loading && !rows.length" class="loading-row">
            <IconLoader2 :size="20" class="spin" /> Cargando productos…
        </div>
        <EmptyState v-else-if="!rows.length" :icon="IconPackage" :title="q ? 'Sin resultados' : 'Todavía no hay productos'"
            :text="q ? 'Probá con otro texto de búsqueda.' : 'Cargá el primero con “Nuevo producto”.'" />
        <div v-else class="table-wrap">
            <table class="table">
                <thead>
                    <tr>
                        <th>Producto</th>
                        <th>Categoría</th>
                        <th class="num">Costo</th>
                        <th class="num">Precio</th>
                        <th class="num">Stock</th>
                        <th class="col-actions"><span class="sr-only">Acciones</span></th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="p in rows" :key="p.id">
                        <td>
                            <div class="cell-main">{{ p.nombre }}</div>
                            <div class="cell-sub">{{ p.codigo || 'Sin código' }}<template v-if="p.descripcion"> · {{ p.descripcion }}</template></div>
                        </td>
                        <td>{{ categoriaNombre(p.categoriaId) }}</td>
                        <td class="num">{{ formatMoney(p.costo) }}</td>
                        <td class="num">{{ formatMoney(p.precio) }}</td>
                        <td class="num">
                            <span class="badge" :class="stockBadge(p.stockMinimo)">
                                {{ formatNumber(p.stockMinimo) }} {{ p.unidadMedida }}
                            </span>
                        </td>
                        <td class="col-actions">
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

    <ProductoForm v-if="formOpen" :producto="editing" @close="formOpen = false" @saved="onSaved" />
    <ConfirmDialog v-if="toDelete" title="Eliminar producto" :busy="deleting"
        :message="`¿Eliminar “${toDelete.nombre}”? Esta acción no se puede deshacer.`" @cancel="toDelete = null"
        @confirm="confirmDelete" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { IconLoader2, IconPackage, IconPencil, IconPlus, IconSearch, IconTrash } from '@tabler/icons-vue'
import PageHeader from '../components/ui/PageHeader.vue'
import AlertBox from '../components/ui/AlertBox.vue'
import EmptyState from '../components/ui/EmptyState.vue'
import ListPager from '../components/ui/ListPager.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import ProductoForm from '../components/forms/ProductoForm.vue'
import { productosApi } from '../api/resources'
import { catalogs } from '../api/catalogs'
import { usePagedList } from '../composables/usePagedList'
import { toast } from '../composables/useToast'
import { formatMoney, formatNumber } from '../utils/format'
import { LIMITS } from '../utils/validation'
import { stockBadge } from '../utils/stock'

const { q, page, pageSize, rows, total, loading, error, reload } = usePagedList(params => productosApi.list(params))

const categorias = ref(new Map())
onMounted(async () => {
    try {
        categorias.value = (await catalogs.categorias.load()).byId
    } catch { /* sin nombres de categoría se muestra el id */ }
})

const categoriaNombre = id => (id == null ? '—' : categorias.value.get(id)?.nombre ?? `#${id}`)

const formOpen = ref(false)
const editing = ref(null)
const toDelete = ref(null)
const deleting = ref(false)

function openForm(producto) {
    editing.value = producto
    formOpen.value = true
}

function onSaved() {
    toast.success(editing.value ? 'Producto actualizado.' : 'Producto creado.')
    formOpen.value = false
    reload()
}

async function confirmDelete() {
    deleting.value = true
    try {
        await productosApi.remove(toDelete.value.id)
        catalogs.productos.invalidate()
        toast.success('Producto eliminado.')
        toDelete.value = null
        reload()
    } catch (e) {
        toast.error(e.message)
    } finally {
        deleting.value = false
    }
}
</script>
