<template>
  <div class="col-12 col-md-4 order-1 order-md-2">
    <ul class="list-group list-group-flush my-3">
      <li class="list-group-item p-2">
        <div class="fw-bold" :style="{ color: uiConfiguration.primaryTextColor }">Author:</div>
        <div :style="{ color: uiConfiguration.secondaryTextColor }">
          {{ author.name }}
        </div>
      </li>
      <li class="list-group-item p-2">
        <div class="fw-bold" :style="{ color: uiConfiguration.primaryTextColor }">Email:</div>
        <span :style="{ color: uiConfiguration.secondaryTextColor }">{{ author.email }}</span>
      </li>
      <li class="list-group-item p-2">
        <div class="fw-bold" :style="{ color: uiConfiguration.primaryTextColor }">
          Negotiation ID:
        </div>
        <span :style="{ color: uiConfiguration.secondaryTextColor }">
          {{ negotiation ? negotiation.id : '' }}</span
        >
      </li>
      <li class="list-group-item p-2">
        <div class="fw-bold" :style="{ color: uiConfiguration.primaryTextColor }">
          Submitted at:
        </div>
        <span :style="{ color: uiConfiguration.secondaryTextColor }">
          {{ negotiation ? printDate(negotiation.creationDate) : '' }}</span
        >
      </li>
      <li class="list-group-item p-2 d-flex justify-content-between">
        <div>
          <div class="fw-bold" :style="{ color: uiConfiguration.primaryTextColor }">Status:</div>
          <span :class="getBadgeColor(negotiation.status)" class="badge py-2 rounded-pill bg"
            ><i :class="getBadgeIcon(negotiation.status)" class="px-1" />
            {{ negotiation ? transformStatus(negotiation.status) : '' }}</span
          >
        </div>
      </li>
      <li
        v-if="negotiation.status !== 'DRAFT' && possibleEvents.length > 0"
        class="list-group-item p-2 d-flex justify-content-between"
      >
        <ul class="list-unstyled mt-1 d-flex flex-row flex-wrap">
          <li v-for="event in possibleEvents" :key="event.label" class="me-2">
            <button
              :class="getButtonColor(event.label)"
              class="btn btn-status mb-1 d-flex text-left"
              data-bs-toggle="modal"
              data-bs-target="#abandonModal"
              @click="assignStatus(event)"
            >
              <i :class="getButtonIcon(event.label)" />
              {{ event.label }}
            </button>
          </li>
        </ul>
      </li>
      <li v-else-if="canDelete()" class="list-group-item p-2 d-flex justify-content-between">
        <ul class="list-unstyled mt-1 d-flex flex-row flex-wrap">
          <li class="me-2">
            <button
              class="btn btn-status bg-danger mb-1 d-flex text-left"
              data-bs-toggle="modal"
              data-bs-target="#negotiationDeleteModal"
            >
              <i class="bi bi-trash" /> DELETE
            </button>
          </li>
        </ul>
      </li>
      <li class="list-group-item p-2 btn-sm border-bottom-0">
        <PDFButton class="mt-2" :negotiation-pdf-data="negotiation" />
        <MergedPDFButton class="mt-2" :negotiation-pdf-data="negotiation" />
        <TransferButton
          class="mt-2"
          :negotiation-id="negotiation.id"
          @transfer-negotiation="handleTransferNegotiation"
        />
      </li>
      <li
        v-if="getSummaryLinks(negotiation._links).length > 0"
        class="list-group-item p-2 border-bottom-0 flex-column d-flex"
      >
        <a
          v-for="link in getSummaryLinks(negotiation._links)"
          :key="link"
          class="cursor-pointer"
          :style="{ color: uiConfiguration.primaryTextColor }"
          @click="downloadAttachmentFromLink(link.href)"
          ><i class="bi bi-filetype-pdf" /> {{ link.title }}</a
        >
      </li>
    </ul>
  </div>
</template>

<script setup>
import PDFButton from '@/components/PDFButton.vue'
import MergedPDFButton from '@/components/MergedPDFButton.vue'
import TransferButton from '@/components/TransferButton.vue'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import {
  getBadgeColor,
  getBadgeIcon,
  getButtonColor,
  getButtonIcon,
  transformStatus,
  formatTimestampToLocalDateTime,
} from '../composables/utils.js'

useNegotiationPageStore()
defineProps({
  negotiation: { type: Object, required: true },
  author: { type: Object, required: true },
  possibleEvents: { type: Array, required: true },
  uiConfiguration: { type: Object, required: true },
  canDelete: { type: Function, required: true },
})

const emit = defineEmits(['assign-status', 'download-attachment-from-link', 'transfer-negotiation'])

function printDate(date) {
  return formatTimestampToLocalDateTime(date)
}

function assignStatus(status) {
  emit('assign-status', status)
}

function downloadAttachmentFromLink(href) {
  emit('download-attachment-from-link', href)
}

function getSummaryLinks(links) {
  const summaryLinks = []
  for (const key in links) {
    if (key.startsWith('Requirement summary')) {
      summaryLinks.push(links[key])
    }
  }
  return summaryLinks
}

function handleTransferNegotiation(subjectId) {
  // Optional: Log or perform cleanup
  console.log(`Negotiation transferred to Subject ID: ${subjectId}`)
  emit('transfer-negotiation', subjectId)
}
</script>

<style scoped>
.btn-status {
  color: white;
  min-width: 100%;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: bold;
  line-height: 1.5;
  border-radius: 3px;
  width: 100px;
  gap: 8px;
}
</style>
