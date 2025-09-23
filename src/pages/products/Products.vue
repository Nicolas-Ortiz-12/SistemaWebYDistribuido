<script setup>
import { ref, computed } from "vue";
import "../../assets/productos.css"; // <-- usa tu CSS específico para esta pantalla

// Lista inicial de productos (puedes cambiar/expandir)
const productos = ref([
    { categoria: "Carpintería", nombre: "Clavo", detalles: "4 mm" },
    { categoria: "Herramientas eléctricas", nombre: "Taladro", detalles: "Batería 20V, 2Ah" },
    { categoria: "Herramientas manuales", nombre: "Martillo", detalles: "Mango de madera 16 oz" },
    { categoria: "Iluminación para el hogar", nombre: "Lámpara LED", detalles: "Ahorro de energía 9W" },
]);

const busqueda = ref("");

// Filtrar productos por nombre, categoría o detalles
const productosFiltrados = computed(() => {
    const q = busqueda.value.trim().toLowerCase();
    if (!q) return productos.value;
    return productos.value.filter(
        (p) =>
            p.nombre.toLowerCase().includes(q) ||
            p.categoria.toLowerCase().includes(q) ||
            p.detalles.toLowerCase().includes(q)
    );
});

// Acciones CRUD simples con prompt (mock)
function agregarProducto() {
    const categoria = prompt("Ingrese la categoría:");
    const nombre = prompt("Ingrese el nombre del producto:");
    const detalles = prompt("Ingrese los detalles:");
    if (categoria && nombre && detalles) {
        productos.value.push({ categoria, nombre, detalles });
    }
}

function editarProducto(index) {
    const curr = productos.value[index];
    const categoria = prompt("Editar categoría:", curr.categoria);
    const nombre = prompt("Editar nombre:", curr.nombre);
    const detalles = prompt("Editar detalles:", curr.detalles);
    if (categoria && nombre && detalles) {
        productos.value[index] = { categoria, nombre, detalles };
    }
}

function eliminarProducto(index) {
    if (confirm("¿Seguro que quieres eliminar este producto?")) {
        productos.value.splice(index, 1);
    }
}
</script>

<template>
    <section class="card productos">
        <div class="card-header">
            <h2 style="margin:0">Lista de productos</h2>
            <button class="btn nuevo" @click="agregarProducto">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" width="18" height="18">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                Nuevo producto
            </button>
        </div>

        <div class="card-body">
            <div class="filters">
                <label class="input" style="grid-column: 1 / -1">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" width="18" height="18">
                        <circle cx="11" cy="11" r="8" />
                        <line x1="21" y1="21" x2="16.65" y2="16.65" />
                    </svg>
                    <input type="text" placeholder="Buscar por nombre, categoría o detalles" v-model="busqueda"
                        class="buscar" />
                </label>
            </div>

            <div class="tabla-container">
                <table>
                    <thead>
                        <tr>
                            <th>Categoría</th>
                            <th>Nombre</th>
                            <th>Detalles</th>
                            <th>Editar</th>
                            <th>Eliminar</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="(prod, index) in productosFiltrados" :key="`${prod.nombre}-${index}`">
                            <td>{{ prod.categoria }}</td>
                            <td>{{ prod.nombre }}</td>
                            <td>{{ prod.detalles }}</td>
                            <td>
                                <button class="btn editar" @click="editarProducto(index)" title="Editar">
                                    ✏️
                                </button>
                            </td>
                            <td>
                                <button class="btn eliminar" @click="eliminarProducto(index)" title="Eliminar">
                                    🗑️
                                </button>
                            </td>
                        </tr>
                        <tr v-if="productosFiltrados.length === 0">
                            <td colspan="5" style="text-align:center; color:#6b7280; padding:12px">
                                Sin resultados para “{{ busqueda }}”
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </section>
</template>
