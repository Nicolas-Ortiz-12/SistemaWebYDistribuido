<template>
  <div v-if="isAuthRoute" class="auth-shell">
    <router-view />
  </div>

  <div v-else class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">F</span>
        <div>
          <strong>Ferreteria</strong>
          <small>Operacion movil</small>
        </div>
      </div>

      <div class="topbar-spacer"></div>

      <div class="session-chip" v-if="username">
        {{ username }}
      </div>
      <button class="topbar-action" type="button" @click="logout">Salir</button>
    </header>

    <nav class="module-nav" aria-label="Modulos principales">
      <router-link to="/productos/lista">Productos</router-link>
      <router-link to="/existencias/compras">Compras</router-link>
      <router-link to="/existencias/ventas">Ventas</router-link>
      <router-link to="/existencias/deudores">Libro de Fiados</router-link>
      <router-link to="/productos/proveedores">Proveedores</router-link>
    </nav>

    <main class="content app-content">
      <router-view />
    </main>

    <nav class="bottom-nav" aria-label="Navegacion principal">
      <router-link to="/productos/lista">
        <span>Productos</span>
      </router-link>
      <router-link to="/existencias/ventas">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-shopping-cart"><circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path></svg>
        <span>Vender</span>
      </router-link>
      <router-link to="/existencias/deudores">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-book"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
        <span>Fiados</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth } from "./composables/useAuth";

const route = useRoute();
const router = useRouter();
const { user, logout: clearSession } = useAuth();

const isAuthRoute = computed(() => route.path.startsWith("/auth"));
const username = computed(() => user.value?.username ?? "");

function logout() {
  clearSession();
  router.push({ name: "login" });
}
</script>
