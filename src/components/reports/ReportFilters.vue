<template>
    <section class="card">
        <div class="card-body stack">
            <div class="chips" role="group" aria-label="Período">
                <button v-for="p in PRESETS" :key="p.key" type="button" class="chip"
                    :class="{ active: report.filters.preset === p.key }" :aria-pressed="report.filters.preset === p.key"
                    @click="report.applyPreset(p.key)">
                    {{ p.label }}
                </button>
            </div>

            <form class="grid grid-4 filters-form" novalidate @submit.prevent="report.run()">
                <FormField v-slot="f" label="Desde">
                    <input :id="f.id" v-model="report.filters.desde" class="input" type="date" :max="report.filters.hasta"
                        @input="report.filters.preset = 'custom'" />
                </FormField>
                <FormField v-slot="f" label="Hasta">
                    <input :id="f.id" v-model="report.filters.hasta" class="input" type="date" :min="report.filters.desde"
                        @input="report.filters.preset = 'custom'" />
                </FormField>
                <FormField v-slot="f" :label="partyLabel">
                    <SearchSelect v-model="report.filters.partyId" :input-id="f.id" :selected-label="report.filters.partyLabel"
                        :search="search" :placeholder="partyPlaceholder"
                        @select="opt => (report.filters.partyLabel = opt?.label ?? '')" />
                </FormField>
                <div class="row filters-actions">
                    <button type="submit" class="btn btn-primary" :disabled="report.loading.value">
                        <IconLoader2 v-if="report.loading.value" :size="18" class="spin" />
                        <IconSearch v-else :size="18" />
                        Consultar
                    </button>
                    <button type="button" class="btn" :disabled="!report.items.value.length" @click="emit('export')">
                        <IconDownload :size="18" /> CSV
                    </button>
                </div>
            </form>

            <AlertBox v-if="report.filterError.value" type="error">{{ report.filterError.value }}</AlertBox>
        </div>
    </section>
</template>

<script setup>
import { IconDownload, IconLoader2, IconSearch } from '@tabler/icons-vue'
import FormField from '../ui/FormField.vue'
import SearchSelect from '../ui/SearchSelect.vue'
import AlertBox from '../ui/AlertBox.vue'
import { PRESETS } from '../../composables/useReport'

defineProps({
    // Instancia de useReport()
    report: { type: Object, required: true },
    partyLabel: { type: String, required: true },
    partyPlaceholder: { type: String, required: true },
    search: { type: Function, required: true },
})
const emit = defineEmits(['export'])
</script>

<style scoped>
.filters-form { align-items: end; }
.filters-actions { padding-bottom: 1px; }
</style>
