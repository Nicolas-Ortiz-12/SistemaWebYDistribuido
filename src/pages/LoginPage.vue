<template>
    <div class="login-page">
        <section class="login-panel">
            <div class="login-brand">
                <span class="login-logo"><IconTool :size="26" /></span>
                <div>
                    <h1>{{ APP_NAME }}</h1>
                    <p class="muted">Sistema de gestión de ventas, compras e inventario</p>
                </div>
            </div>

            <form class="login-form" novalidate @submit.prevent="onSubmit">
                <h2>Iniciar sesión</h2>

                <AlertBox v-if="error" type="error">{{ error }}</AlertBox>
                <AlertBox v-else-if="expired" type="info">Tu sesión expiró. Volvé a ingresar.</AlertBox>

                <div class="field">
                    <label class="field-label" for="login-user">Usuario</label>
                    <div class="input-icon">
                        <IconUser :size="18" />
                        <input id="login-user" v-model="username" class="input" type="text" autocomplete="username"
                            autocapitalize="off" spellcheck="false" :maxlength="LIMITS.login.username" required
                            autofocus :disabled="loading" />
                    </div>
                </div>

                <div class="field">
                    <label class="field-label" for="login-pass">Contraseña</label>
                    <div class="input-icon">
                        <IconLock :size="18" />
                        <input id="login-pass" v-model="password" class="input login-pass"
                            :type="showPassword ? 'text' : 'password'" autocomplete="current-password"
                            :maxlength="LIMITS.login.password" required :disabled="loading"
                            @keyup="checkCapsLock" @keydown="checkCapsLock" />
                        <button type="button" class="icon-btn login-toggle"
                            :aria-label="showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                            :aria-pressed="showPassword" @click="showPassword = !showPassword">
                            <IconEyeOff v-if="showPassword" :size="18" />
                            <IconEye v-else :size="18" />
                        </button>
                    </div>
                    <p v-if="capsLock" class="field-hint login-caps">
                        <IconAlertTriangle :size="14" /> Bloq Mayús está activado
                    </p>
                </div>

                <button class="btn btn-primary login-submit" type="submit" :disabled="!canSubmit">
                    <IconLoader2 v-if="loading" :size="18" class="spin" />
                    <IconLogin2 v-else :size="18" />
                    {{ loading ? 'Ingresando…' : 'Ingresar' }}
                </button>
            </form>
        </section>
    </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
    IconAlertTriangle, IconEye, IconEyeOff, IconLoader2, IconLock, IconLogin2, IconTool, IconUser,
} from '@tabler/icons-vue'
import AlertBox from '../components/ui/AlertBox.vue'
import { AuthError, login } from '../auth/session'
import { APP_NAME } from '../config'
import { cleanText, LIMITS, safeRedirect } from '../utils/validation'

const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const showPassword = ref(false)
const capsLock = ref(false)
const loading = ref(false)
const error = ref('')

const expired = computed(() => route.query.expired === '1')
const canSubmit = computed(() => !loading.value && cleanText(username.value) !== '' && password.value !== '')

function checkCapsLock(event) {
    capsLock.value = event.getModifierState?.('CapsLock') ?? false
}

async function onSubmit() {
    if (!canSubmit.value) return
    error.value = ''
    loading.value = true
    try {
        // La contraseña se manda tal cual: recortarla cambiaría la credencial
        await login(cleanText(username.value), password.value)
        router.replace(safeRedirect(route.query.redirect))
    } catch (e) {
        error.value = e instanceof AuthError ? e.message : 'No se pudo iniciar sesión.'
        password.value = ''
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.login-page {
    min-height: 100vh;
    display: grid;
    place-items: center;
    padding: 24px 16px;
    background: #0f1f2e;
}

.login-panel {
    width: 100%;
    max-width: 360px;
    background: var(--surface);
    border-radius: var(--radius);
    box-shadow: var(--shadow-lg);
    overflow: hidden;
}

.login-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 14px 16px;
    background: var(--surface-alt);
    border-bottom: 1px solid var(--border);
}

.login-logo {
    display: grid;
    place-items: center;
    width: 38px;
    height: 38px;
    border-radius: var(--radius);
    background: var(--action);
    color: #fff;
    flex-shrink: 0;
}

.login-form { display: flex; flex-direction: column; gap: 10px; padding: 14px 16px 16px; }

.login-pass { padding-right: 42px; }

.login-toggle { position: absolute; right: 4px; }

.login-caps { display: flex; align-items: center; gap: 4px; color: var(--warning); }

.login-submit { height: 38px; margin-top: 2px; }
</style>
