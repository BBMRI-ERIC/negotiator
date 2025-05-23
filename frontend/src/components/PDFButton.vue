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
import { computed } from 'vue'
import jsPDF from 'jspdf'
import { applyPlugin } from 'jspdf-autotable'

import { useUiConfiguration } from '../store/uiConfiguration.js'
import { useNegotiationPageStore } from '@/store/negotiationPage.js'

applyPlugin(jsPDF)

const props = defineProps({
  negotiationPdfData: {
    type: Object,
    default: undefined
  }
})

const uiConfigurationStore = useUiConfiguration()
const negotiationPageStore = useNegotiationPageStore()


const uiConfigurationTheme = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

async function retrievePDF() {
  try {
    const pdfData = await negotiationPageStore.retrieveNegotiationPDF(props.negotiationPdfData.id)
    const pdfBlob = new Blob([pdfData], { type: 'application/pdf' })

    const link = document.createElement('a')
    link.href = URL.createObjectURL(pdfBlob)
    link.download = `Negotiation_${props.negotiationPdfData.id}.pdf`
    document.body.appendChild(link);
    link.click()
    document.body.removeChild(link);

    URL.revokeObjectURL(link.href)
  } catch (error) {
    console.error('Error retrieving or saving the PDF:', error)
  }
}

</script>

<style scoped>
a {
  text-decoration: none;
}

.pdf-text-hover:hover,
.pdf-text-hover:hover i {
  color: #dc3545 !important; /* Bootstrap's danger red */
}
</style>
