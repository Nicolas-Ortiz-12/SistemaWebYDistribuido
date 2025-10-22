import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../pages/Dashboard.vue'
import Clients from '../pages/Clients.vue'

import ProductsLayout from '../pages/products/ProductsLayout.vue'
import Products from '../pages/products/Products.vue'
import Categories from '../pages/products/Categories.vue'
import Suppliers from '../pages/Suppliers.vue'

// Inventario / Existencias
import InventoryLayout from '../pages/inventory/InventoryLayout.vue'
import NewPurchase from '../pages/inventory/NewPurchase.vue'
import Sales from '../pages/inventory/Sales.vue'

import Reports from '../pages/reports/Reports.vue'

export default createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', name: 'dashboard', component: Dashboard },
        { path: '/clientes', name: 'clientes', component: Clients },

        // Gestión de Productos
        {
            path: '/productos', component: ProductsLayout, children: [
                { path: '', redirect: '/productos/categorias' },
                { path: 'categorias', name: 'categorias', component: Categories },
                { path: 'lista', name: 'productos', component: Products },
                { path: 'proveedores', name: 'proveedores', component: Suppliers },
            ]
        },

        // Gestión de Existencias
        {
            path: '/existencias', component: InventoryLayout, children: [
                { path: '', redirect: '/existencias/stock' },
                { path: 'compras', name: 'compras', component: NewPurchase },
                { path: 'ventas', name: 'ventas', component: Sales },
            ]
        },

        { path: '/reportes', name: 'reportes', component: Reports },
        { path: '/usuarios', component: Dashboard },
        { path: '/configuracion', component: Dashboard },
    ]
})