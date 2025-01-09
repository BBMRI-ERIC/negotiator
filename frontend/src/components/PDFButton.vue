<template>
  <div>
    <a
      class="pdf-text-hover cursor-pointer"
      @click="createPDF"
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
// eslint-disable-next-line
import autoTable from 'jspdf-autotable'
import bbmriLogo from '../assets/images/bbmri/nav-bar-bbmri.png'
import eucaimLogo from '../assets/images/eucaim/home-eucaim.png'
import canservLogo from '../assets/images/canserv/nav-bar-canserv.png'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import moment from 'moment'
import { dateFormat } from '@/config/consts'
import { transformStatus } from '../composables/utils.js'

const props = defineProps({
  negotiationPdfData: {
    type: Object,
    default: undefined,
  },
})

const uiConfigurationStore = useUiConfiguration()

const uiConfigurationNavbar = computed(() => {
  return uiConfigurationStore.uiConfiguration?.navbar
})

const uiConfigurationTheme = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

const returnLogoSrc = computed(() => {
  if (uiConfigurationNavbar.value?.navbarLogoUrl === 'bbmri') {
    return bbmriLogo
  } else if (uiConfigurationNavbar.value?.navbarLogoUrl === 'canserv') {
    return canservLogo
  } else if (uiConfigurationNavbar.value?.navbarLogoUrl === 'eucaim') {
    return eucaimLogo
  }
  return uiConfigurationNavbar.value?.navbarLogoUrl
})

function createPDF() {
  const pdfName = 'negotiation'
  const doc = new jsPDF({ compress: true })

  const negotiationUser = {
    Author: props.negotiationPdfData.author.name,
    Email: props.negotiationPdfData.author.email,
    'Negotiation ID': props.negotiationPdfData.id,
    'Submitted at': moment(props.negotiationPdfData.creationDate).format(dateFormat),
    Status: transformStatus(props.negotiationPdfData.status),
    'Report generated at': moment().format(dateFormat),
  }

  doc.addImage(returnLogoSrc.value, 'JPEG', 15, 7, 50, 13, 'FAST')

  doc.autoTable({
    body: [['REQUEST SUMMARY']],
    columnStyles: {
      0: { font: 'calibri', fontStyle: 'bold', halign: 'center' },
    },
    startY: 25,
    rowPageBreak: 'auto',
    bodyStyles: { valign: 'top' },
  })

  for (const [key, value] of Object.entries(negotiationUser)) {
    doc.autoTable({
      body: [[`${key}:`, `${value}`]],
      columnStyles: {
        0: { cellWidth: 23, font: 'calibri', fontStyle: 'bold' },
        1: { cellWidth: 100, font: 'calibri' },
      },
      theme: 'plain',
      startY: doc.lastAutoTable.finalY + 2,
      rowPageBreak: 'auto',
      bodyStyles: { valign: 'top' },
    })
  }

  for (const key in props.negotiationPdfData.payload) {
    doc.autoTable({
      body: [[key.toUpperCase()]],
      columnStyles: {
        0: { font: 'calibri', fontStyle: 'bold' },
      },
      startY: doc.lastAutoTable.finalY + 10,
      rowPageBreak: 'auto',
      bodyStyles: { valign: 'top' },
    })

    for (const value in props.negotiationPdfData.payload[key]) {
      if (props.negotiationPdfData.payload[key][value]) {
        doc.autoTable({
          body: [[value + ': ', props.negotiationPdfData.payload[key][value]]],
          startY: doc.lastAutoTable.finalY + 2,
          columnStyles: {
            0: { cellWidth: 25, font: 'calibri', fontStyle: 'bold' },
            1: { cellWidth: 100, font: 'calibri' },
          },
          theme: 'plain',
          rowPageBreak: 'auto',
          bodyStyles: { valign: 'top' },
        })
      }
    }
  }
  doc.save(pdfName + '.pdf')
}
</script>

<style scoped>
a {
  text-decoration: none;
}

.pdf-text-hover:hover {
  color: var(--hoverColor) !important;
}
</style>
