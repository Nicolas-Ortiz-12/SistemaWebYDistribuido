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

                    <button class="btn" :disabled="loading">
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
import { useRouter } from "vue-router";
import { login } from "../../services/authService";
import { useAuth } from "../../composables/useAuth";

const router = useRouter();
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

        // API devuelve access_token, refresh_token, expires_in_minutes, token_type
        // guardamos en localStorage de forma segura según el flag remember
        localStorage.setItem("access_token", data.access_token);
        localStorage.setItem("refresh_token", data.refresh_token || "");
        localStorage.setItem("token_type", data.token_type || "Bearer");
        localStorage.setItem("expires_in_minutes", String(data.expires_in_minutes || 60));
        // opcional: guardar username para mostrar en UI
        localStorage.setItem("username", username.value);

        // actualizar estado global de auth (composable)
        setUser({ username: username.value });

        // redirigir a dashboard o la ruta protegida
        router.push({ name: "dashboard" });
    } catch (e) {
        // muestra mensaje limpio
        error.value = e?.message || "Error al iniciar sesión";
    } finally {
        loading.value = false;
    }
}
</script>

<style src="../../assets/auth.css" scoped></style>
