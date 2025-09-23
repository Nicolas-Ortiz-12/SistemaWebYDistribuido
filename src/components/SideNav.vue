<template>
    <aside>
        <div class="profile">
            <div class="avatar">D</div>
            <div>
                <h3>Nicolas Ortiz</h3>
                <p>nicolasortiz@gmail.com</p>
            </div>
        </div>

        <nav class="nav">
            <h4>Navegación principal</h4>

            <RouterLink to="/">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M3 12l2-2 7-7 7 7 2 2v8a2 2 0 0 1-2 2h-4v-6H9v6H5a2 2 0 0 1-2-2z" />
                </svg>
                Dashboard
            </RouterLink>

            <RouterLink to="/clientes">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                    <circle cx="9" cy="7" r="4" />
                    <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
                    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
                Clientes
            </RouterLink>

            <!-- Grupo: Gestión de Productos -->
            <button class="nav-group" @click="toggleProductos">
                <span class="icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <rect x="3" y="3" width="7" height="7" />
                        <rect x="14" y="3" width="7" height="7" />
                        <rect x="14" y="14" width="7" height="7" />
                        <rect x="3" y="14" width="7" height="7" />
                    </svg>
                </span>
                Gestión de Productos
                <span class="caret" :class="{ open: openProductos }">▾</span>
            </button>
            <div v-show="openProductos" class="subnav">
                <RouterLink to="/productos/categorias">Categorías</RouterLink>
                <RouterLink to="/productos/lista">Productos</RouterLink>
                <RouterLink to="/productos/proveedores">Proveedores</RouterLink>
            </div>



            <!-- Grupo: Gestión de Existencias -->
            <button class="nav-group" @click="toggleExistencias">
                <span class="icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                        <polyline points="7 10 12 15 17 10" />
                        <line x1="12" y1="15" x2="12" y2="3" />
                    </svg>
                </span>
                Gestión de Existencias
                <span class="caret" :class="{ open: openExistencias }">▾</span>
            </button>
            <div v-show="openExistencias" class="subnav">
                <RouterLink to="/existencias/compras">Ingresar por factura</RouterLink>
                <RouterLink to="/existencias/ventas">Ventas / Facturación</RouterLink>
            </div>

            <RouterLink to="/usuarios">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M20 21v-2a4 4 0 0 0-3-3.87" />
                    <path d="M4 21v-2a4 4 0 0 1 3-3.87" />
                    <circle cx="12" cy="7" r="4" />
                </svg>
                Gestión de usuarios
            </RouterLink>

            <RouterLink to="/reportes">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M3 3v18h18" />
                    <path d="M19 9l-5 5-4-4-6 6" />
                </svg>
                Reportes
            </RouterLink>

            <RouterLink to="/configuracion">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <circle cx="12" cy="12" r="3" />
                    <path
                        d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1z" />
                </svg>
                Configuración
            </RouterLink>
        </nav>
    </aside>
</template>

<script setup>
import { RouterLink, useRoute } from 'vue-router'
import { ref, watchEffect } from 'vue'

const route = useRoute()

const openProductos = ref(false)
const openExistencias = ref(false)

// Abre automáticamente el grupo según la ruta actual
watchEffect(() => {
    const p = route.path
    openProductos.value = p.startsWith('/productos')
    openExistencias.value = p.startsWith('/existencias')
})

function toggleProductos() {
    openProductos.value = !openProductos.value
}
function toggleExistencias() {
    openExistencias.value = !openExistencias.value
}
</script>

<style scoped>
.nav-group {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 10px;
    background: #f3f4f6;
    border: none;
    width: 100%;
    text-align: left;
    color: #374151;
    font: inherit;
    cursor: pointer;
    margin: 6px 0
}

.nav-group .icon svg {
    width: 18px;
    height: 18px
}

.caret {
    margin-left: auto;
    transition: transform .15s ease
}

.caret.open {
    transform: rotate(180deg)
}

.subnav {
    display: flex;
    flex-direction: column;
    margin: 4px 0 10px 30px
}

.subnav a {
    padding: 8px 10px;
    border-radius: 8px
}
</style>
