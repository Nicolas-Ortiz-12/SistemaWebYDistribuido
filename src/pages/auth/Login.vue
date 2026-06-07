<template>
    <div class="login-page">
        <div class="card">
            <h1 class="title">Iniciar sesión</h1>

            <form @submit.prevent="submit" class="form">
                <label class="label">Usuario</label>
                <input v-model="username" class="input" type="text" autocomplete="username" required />

                <label class="label">Contraseña</label>
                <input v-model="password" class="input" type="password" autocomplete="current-password" required />

                <div class="row">
                    <label class="remember">
                        <input type="checkbox" v-model="remember" />
                        Recordarme
                    </label>

                    <button type="submit" class="btn btn-primary" :disabled="loading">
                        <span v-if="loading">Ingresando...</span>
                        <span v-else>Ingresar</span>
                    </button>
                </div>

                <p v-if="error" class="error">{{ error }}</p>
            </form>

            <p class="small">
                ¿No tienes cuenta? <router-link to="/register">Regístrate</router-link>
            </p>
        </div>
    </div>
</template>

<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { login } from "../../services/authService";
import { useAuth } from "../../composables/useAuth";
import { saveAuthTokens } from "../../services/authStorage";

const router = useRouter();
const route = useRoute();
const { setUser } = useAuth();

const username = ref("");
const password = ref("");
const remember = ref(false);
const loading = ref(false);
const error = ref("");

async function submit() {
    error.value = "";
    loading.value = true;
    try {
        const data = await login({ username: username.value, password: password.value });
        saveAuthTokens(data, { remember: remember.value, username: username.value });

        // actualizar estado global de auth (composable)
        setUser({ username: username.value });

        // redirigir al modulo principal o la ruta protegida
        const redirect = typeof route.query.redirect === "string" && route.query.redirect
            ? route.query.redirect
            : { name: "productos" };
        router.push(redirect);
    } catch (e) {
        // muestra mensaje limpio
        error.value = e?.message || "Error al iniciar sesión";
    } finally {
        loading.value = false;
    }
}
</script>

<style src="../../assets/auth.css" scoped></style>
