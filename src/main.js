import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { onSessionExpired } from './auth/session'
import '@fontsource/ibm-plex-mono/latin-500.css'
import '@fontsource/ibm-plex-mono/latin-700.css'
import './assets/main.css'

// Si el backend rechaza el token y no se puede renovar, se vuelve al login conservando la ruta
onSessionExpired(() => {
    const current = router.currentRoute.value
    if (current.name !== 'login') {
        router.replace({ name: 'login', query: { redirect: current.fullPath, expired: '1' } })
    }
})

createApp(App).use(router).mount('#app')
