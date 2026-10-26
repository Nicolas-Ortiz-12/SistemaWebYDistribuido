<script setup>
import { ref, watch, onUnmounted, nextTick } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'scan'])

const errorMsg = ref('')
let html5QrCode = null

const startScanner = async () => {
  await nextTick()
  html5QrCode = new Html5Qrcode("reader")
  try {
    await html5QrCode.start(
      { facingMode: "environment" },
      { 
        fps: 10, 
        qrbox: { width: 250, height: 150 },
        aspectRatio: 1.0,
      },
      (decodedText) => {
        emit('scan', decodedText)
        closeScanner()
      },
      (err) => {
        // Ignoramos errores de escaneo continuo
      }
    )
  } catch (err) {
    console.error("Error al iniciar cámara:", err)
    errorMsg.value = "No se pudo acceder a la cámara. Revisa los permisos."
  }
}

const stopScanner = async () => {
  if (html5QrCode && html5QrCode.isScanning) {
    try {
      await html5QrCode.stop()
      html5QrCode.clear()
    } catch(e) {
      console.error("Error parando escáner", e)
    }
  }
  html5QrCode = null
}

watch(() => props.show, (newVal) => {
  if (newVal) {
    startScanner()
  } else {
    stopScanner()
  }
}, { immediate: true })

const closeScanner = () => {
  emit('close')
}

onUnmounted(() => {
  stopScanner()
})
</script>

<template>
  <div class="modal fade-in" v-if="show" tabindex="-1" role="dialog" @keydown.esc="closeScanner">
    <div class="backdrop fade-in-backdrop" @click="closeScanner"></div>
    <div class="card modal-card scale-in" role="document">
      <div class="card-header premium-header">
        <div class="header-title-area">
          <svg class="header-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
            <circle cx="12" cy="13" r="4" />
          </svg>
          <h3>Escanear Código</h3>
        </div>
        <button class="icon-btn close-btn" @click="closeScanner" title="Cerrar" aria-label="Cerrar">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
      <div class="card-body modal-body-scroll" style="display:flex; flex-direction:column; align-items:center; justify-content:center;">
        <p style="margin-bottom: 1rem; text-align: center; color: var(--text-muted);">
          Apunta la cámara al código de barras del producto.
        </p>
        <div v-if="errorMsg" class="error-banner fade-in" style="width: 100%;">
          <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <span>{{ errorMsg }}</span>
        </div>
        <div id="reader" style="width: 100%; max-width: 400px; border-radius: var(--radius-md); overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
#reader {
  background: var(--bg-body);
}
</style>
