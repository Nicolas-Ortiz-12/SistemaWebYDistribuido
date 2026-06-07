import { createRouter, createWebHistory } from "vue-router";
import { useAuth } from "./composables/useAuth";

import Login from "./pages/auth/Login.vue";
import Suppliers from "./pages/Suppliers.vue";
import Products from "./pages/products/Products.vue";
import Sales from "./pages/inventory/Sales.vue";
import NewPurchase from "./pages/inventory/NewPurchase.vue";

import DebtorsBook from "./pages/inventory/DebtorsBook.vue";

const routes = [
  { path: "/", redirect: { name: "productos" } },
  { path: "/auth/login", name: "login", component: Login },
  { path: "/register", redirect: { name: "login" } },
  { path: "/productos/lista", name: "productos", component: Products, meta: { requiresAuth: true } },
  { path: "/productos/proveedores", name: "proveedores", component: Suppliers, meta: { requiresAuth: true } },
  { path: "/existencias/compras", name: "compras", component: NewPurchase, meta: { requiresAuth: true } },
  { path: "/existencias/ventas", name: "ventas", component: Sales, meta: { requiresAuth: true } },
  { path: "/existencias/deudores", name: "deudores", component: DebtorsBook, meta: { requiresAuth: true } },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  if (to.name === "login") return true;
  if (!to.matched.some((record) => record.meta?.requiresAuth)) return true;

  const auth = useAuth();
  try {
    await auth.ensureTokenFresh();
    return true;
  } catch {
    return { name: "login", query: { redirect: to.fullPath } };
  }
});

export default router;
