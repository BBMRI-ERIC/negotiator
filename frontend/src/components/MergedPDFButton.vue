<template>
  <div>
    <a
      class="merged-pdf-button pdf-text-hover cursor-pointer"
      @click="retrieveMergedPDF"
      :style="{
        color: uiConfigurationTheme.primaryTextColor,
        '--hoverColor': uiConfigurationTheme?.secondaryTextColor,
      }"
      ><i class="bi bi-file-earmark-pdf" /> Download Merged PDF</a
    >
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import { useNegotiationPageStore } from '@/store/negotiationPage.js'
import { useNotificationsStore } from '../store/notifications'

const props = defineProps({
  negotiationPdfData: {
    type: Object,
    default: undefined,
  },
})

const uiConfigurationStore = useUiConfiguration()
const negotiationPageStore = useNegotiationPageStore()
const notificationsStore = useNotificationsStore()
const loadingPdf = ref(false)

const uiConfigurationTheme = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

async function retrieveMergedPDF() {
  if (!loadingPdf.value) {
    loadingPdf.value = true
    try {
      const pdfData = await negotiationPageStore.retrieveNegotiationMergedPDF(
        props.negotiationPdfData.id,
      )
      const pdfBlob = new Blob([pdfData], { type: 'application/pdf' })

      const link = document.createElement('a')
      link.href = URL.createObjectURL(pdfBlob)
      link.download = `Negotiation_${props.negotiationPdfData.id}_merged.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      URL.revokeObjectURL(link.href)

      notificationsStore.setNotification('Merged PDF successfully saved', 'success')
      loadingPdf.value = false
    } catch (error) {
      console.error('Error retrieving or saving the merged PDF:', error)
      notificationsStore.setNotification('Error saving merged PDF', 'warning')
      loadingPdf.value = false
    }
  }
}
</script>

<style scoped>
a {
  text-decoration: none;
}

.pdf-text-hover:hover,
.pdf-text-hover:hover i {
  color: var(--bs-danger) !important;
}
</style>
