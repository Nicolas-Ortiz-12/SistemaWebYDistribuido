import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../auth/session'
import { APP_NAME } from '../config'

// Las páginas se cargan bajo demanda: cada una queda en su propio archivo JS
const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/login', name: 'login', component: () => import('../pages/LoginPage.vue'), meta: { public: true, title: 'Ingresar' } },
        { path: '/', name: 'dashboard', component: () => import('../pages/DashboardPage.vue'), meta: { title: 'Dashboard' } },
        { path: '/clientes', name: 'clientes', component: () => import('../pages/ClientesPage.vue'), meta: { title: 'Clientes' } },

        { path: '/productos', redirect: '/productos/lista' },
        { path: '/productos/lista', name: 'productos', component: () => import('../pages/ProductosPage.vue'), meta: { title: 'Productos' } },
        { path: '/productos/categorias', name: 'categorias', component: () => import('../pages/CategoriasPage.vue'), meta: { title: 'Categorías' } },
        { path: '/productos/proveedores', name: 'proveedores', component: () => import('../pages/ProveedoresPage.vue'), meta: { title: 'Proveedores' } },

        { path: '/existencias', redirect: '/existencias/ventas' },
        { path: '/existencias/ventas', name: 'ventas', component: () => import('../pages/VentaNuevaPage.vue'), meta: { title: 'Nueva venta' } },
        { path: '/existencias/compras', name: 'compras', component: () => import('../pages/CompraNuevaPage.vue'), meta: { title: 'Ingresar compra' } },

        { path: '/reportes', name: 'reportes', component: () => import('../pages/reports/ReportesPage.vue'), meta: { title: 'Reportes' } },

        { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../pages/NotFoundPage.vue'), meta: { title: 'Página no encontrada' } },
    ],
})

// Toda ruta que no sea pública requiere sesión iniciada
router.beforeEach(to => {
    if (to.meta.public) {
        return to.name === 'login' && isAuthenticated() ? { path: '/' } : true
    }
    if (!isAuthenticated()) {
        return { name: 'login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} }
    }
    return true
})

router.afterEach(to => {
    document.title = to.meta.title ? `${to.meta.title} · ${APP_NAME}` : APP_NAME
})

export default router
