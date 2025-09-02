<template>
  <div>
    <a
      class="pdf-button pdf-text-hover cursor-pointer"
      @click="retrievePDF"
      :style="{
        color: uiConfigurationTheme.primaryTextColor,
        '--hoverColor': uiConfigurationTheme?.secondaryTextColor,
      }"
      ><i class="bi bi-filetype-pdf" /> Download PDF</a
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

async function retrievePDF() {
  if (!loadingPdf.value) {
    loadingPdf.value = true
    try {
      const pdfData = await negotiationPageStore.retrieveNegotiationPDF(props.negotiationPdfData.id)
      const pdfBlob = new Blob([pdfData], { type: 'application/pdf' })

      const link = document.createElement('a')
      link.href = URL.createObjectURL(pdfBlob)
      link.download = `Negotiation_${props.negotiationPdfData.id}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      URL.revokeObjectURL(link.href)

      notificationsStore.setNotification('File successfully saved', 'success')
      loadingPdf.value = false
    } catch (error) {
      console.error('Error retrieving or saving the PDF:', error)
      notificationsStore.setNotification('Error saving file', 'warning')
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
