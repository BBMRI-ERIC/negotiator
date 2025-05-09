<template>
  <div>
    <a
      class="pdf-button pdf-text-hover cursor-pointer"
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
import { applyPlugin } from 'jspdf-autotable'

import bbmriLogo from '../assets/images/bbmri/nav-bar-bbmri.png'
import eucaimLogo from '../assets/images/eucaim/home-eucaim.png'
import canservLogo from '../assets/images/canserv/nav-bar-canserv.png'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import { useNotificationsStore } from '../store/notifications'
import moment from 'moment'
import { dateFormat } from '@/config/consts'
import { transformStatus } from '../composables/utils.js'

applyPlugin(jsPDF)

const props = defineProps({
  negotiationPdfData: {
    type: Object,
    default: undefined,
  },
})

const uiConfigurationStore = useUiConfiguration()
const notificationsStore = useNotificationsStore()

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
  try {
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
        0: { font: 'Times', fontStyle: 'bold', halign: 'center' },
      },
      startY: 25,
      rowPageBreak: 'auto',
      bodyStyles: { valign: 'top' },
    })

    for (const [key, value] of Object.entries(negotiationUser)) {
      doc.autoTable({
        body: [[`${key}:`, `${value}`]],
        columnStyles: {
          0: { cellWidth: 23, font: 'Times', fontStyle: 'bold' },
          1: { columnWidth: 100, halign: 'left', font: 'Times' },
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
          0: { font: 'Times', fontStyle: 'bold' },
        },
        startY: doc.lastAutoTable.finalY + 10,
        rowPageBreak: 'auto',
        bodyStyles: { valign: 'top' },
      })

      for (const value in props.negotiationPdfData.payload[key]) {
        if (props.negotiationPdfData.payload[key][value]) {
          let payloadValue = props.negotiationPdfData.payload[key][value]

          // Check if the value is attachment and display the name if it exists
          if (props.negotiationPdfData.payload[key][value].name)
            payloadValue = props.negotiationPdfData.payload[key][value].name

          // transform the value to a string
          payloadValue = formatString(payloadValue)

          doc.autoTable({
            body: [[value + ': ', payloadValue]],
            startY: doc.lastAutoTable.finalY + 2,
            columnStyles: {
              0: { cellWidth: 25, font: 'Times', fontStyle: 'bold' },
              1: { columnWidth: 100, halign: 'left', font: 'Times' },
            },
            theme: 'plain',
            rowPageBreak: 'auto',
            bodyStyles: { valign: 'top' },
          })
        }
      }
    }

    doc
      .save(pdfName + '.pdf', { returnPromise: true })
      .then(() => {
        notificationsStore.setNotification('File successfully saved', 'success')
      })
      .catch(() => {
        notificationsStore.setNotification('Error saving file', 'warning')
      })
  } catch (error) {
    console.log(error)
    notificationsStore.setNotification('Error saving file', 'warning')
  }
}

function formatString(str) {
  str = str.toString().replace(/,/g, ', ')
  return str.toString().replace(/_/g, ' ')
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
