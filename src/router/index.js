import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../pages/Dashboard.vue'
import Clients from '../pages/Clients.vue'

import ProductsLayout from '../pages/products/ProductsLayout.vue'
import Products from '../pages/products/Products.vue'
import Categories from '../pages/products/Categories.vue'
import Suppliers from '../pages/Suppliers.vue'

/* Inventario / Existencias */
import InventoryLayout from '../pages/inventory/InventoryLayout.vue'
import NewPurchase from '../pages/inventory/NewPurchase.vue'
import Sales from '../pages/inventory/Sales.vue'

import Reports from '../pages/reports/Reports.vue'
import Login from '../pages/auth/Login.vue' // <- componente de login que creamos
import MainLayout from '../Layout/MainLayout.vue'
import AuthLayout from '../Layout/AuthLayout.vue'
// composable de auth (asegura token / refresh)
import { useAuth } from '../composables/useAuth'

const routes = [
    {
        path: '/auth',
        component: AuthLayout,
        children: [
            { path: 'login', name: 'login', component: Login },
        ]
    },
    {
        path: '/',
        component: MainLayout,
        children: [

            { path: '/', name: 'dashboard', component: Dashboard, meta: { requiresAuth: true } },
            { path: '/clientes', name: 'clientes', component: Clients, meta: { requiresAuth: true } },

            // Gestión de Productos (protegido)
            {
                path: '/productos',
                component: ProductsLayout,
                meta: { requiresAuth: true },
                children: [
                    { path: '', redirect: '/productos/categorias' },
                    { path: 'categorias', name: 'categorias', component: Categories },
                    { path: 'lista', name: 'productos', component: Products },
                    { path: 'proveedores', name: 'proveedores', component: Suppliers },
                ]
            },

            // Gestión de Existencias (protegido)
            {
                path: '/existencias',
                component: InventoryLayout,
                meta: { requiresAuth: true },
                children: [
                    { path: '', redirect: '/existencias/compras' },
                    { path: 'compras', name: 'compras', component: NewPurchase },
                    { path: 'ventas', name: 'ventas', component: Sales },
                ]
            },

            { path: '/reportes', name: 'reportes', component: Reports, meta: { requiresAuth: true } },
            { path: '/usuarios', component: Dashboard, meta: { requiresAuth: true } },
            { path: '/configuracion', component: Dashboard, meta: { requiresAuth: true } },

        ]
    },
    // catch-all: redirige a dashboard o login según auth
    { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

/**
 * Global navigation guard:
 * - si la ruta no requiere auth -> next()
 * - si requiere -> intenta asegurar token (ensureTokenFresh)
 *    - si ok -> setUser (si existe username en localStorage) y next()
 *    - si falla -> redirect a /login?redirect=<ruta>
 */
router.beforeEach(async (to, from, next) => {
    const auth = useAuth()
    const requires = to.matched.some(r => r.meta && r.meta.requiresAuth)

    if (!requires) return next()

    try {
        // intenta refrescar/asegurar token (lanzará si no es posible)
        await auth.ensureTokenFresh()

        // sincroniza estado de usuario si existe username guardado
        const username = localStorage.getItem('username')
        if (username) auth.setUser({ username })

        return next()
    } catch (err) {
        // redirige a login y preserva la ruta destino en query.redirect
        return next({ name: 'login', query: { redirect: to.fullPath } })
    }
})

export default router
