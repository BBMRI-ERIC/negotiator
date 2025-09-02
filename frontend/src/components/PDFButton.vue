<template>
  <downloading-spinner
    id="downloadingPDFSpinner"
  />

  <div>
    <a
      class="pdf-button pdf-text-hover cursor-pointer"
      @click="retrievePDF"
      :style="{
        color: uiConfigurationTheme.primaryTextColor,
        '--hoverColor': uiConfigurationTheme?.secondaryTextColor,
      }"
      ><i class="bi bi-file-earmark-pdf" />{{ text }}</a
    >
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import { useNegotiationPageStore } from '@/store/negotiationPage.js'
import { useNotificationsStore } from '../store/notifications'
import DownloadingSpinner from '@/components/modals/DownloadingSpinner.vue'
import * as bootstrap from "bootstrap";

const props = defineProps({
  negotiationPdfData: {
    type: Object,
    default: undefined,
  },
  text: {
    type: String,
    required: true
  },
  includeAttachments: {
    type: Boolean,
    required: false,
    default: false
  }
})

const uiConfigurationStore = useUiConfiguration()
const negotiationPageStore = useNegotiationPageStore()
const notificationsStore = useNotificationsStore()

const uiConfigurationTheme = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

async function retrievePDF() {
  const spinnerModalEl = document.getElementById("downloadingPDFSpinner");
  const spinnerModal = new bootstrap.Modal(spinnerModalEl);
  spinnerModal.show();
  try {
    const pdfData = await negotiationPageStore.retrieveNegotiationPDF(props.negotiationPdfData.id, props.includeAttachments)
    const pdfBlob = new Blob([pdfData.data], { type: 'application/pdf' })
    
    const link = document.createElement('a')
    link.href = URL.createObjectURL(pdfBlob)
    link.download = pdfData.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)

    notificationsStore.setNotification('File successfully saved', 'success')
    document.getElementById("downloadingPDFSpinner");
  } catch (error) {
    console.error('Error retrieving or saving the PDF:', error)
    notificationsStore.setNotification('Error saving file', 'warning')
  } finally {
    spinnerModal.hide();
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
