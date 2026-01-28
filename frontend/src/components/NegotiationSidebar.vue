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
          {{ $t('negotiationPage.displayId') }}:
        </div>
        <div class="d-flex align-items-center gap-2">
          <span v-if="!isEditingDisplayId" :style="{ color: uiConfiguration.secondaryTextColor }">
            {{ negotiation ? negotiation.displayId : '' }}
          </span>
          <input
            v-else
            v-model="editedDisplayId"
            type="text"
            class="form-control form-control-sm"
            maxlength="20"
            :style="{ color: uiConfiguration.secondaryTextColor }"
            @keyup.enter="saveDisplayId"
            @keyup.esc="cancelEditDisplayId"
          />
          <button
            v-if="isAdmin && !isEditingDisplayId"
            class="btn btn-sm btn-link p-0"
            :style="{ color: uiConfiguration.primaryTextColor }"
            @click="startEditDisplayId"
            title="Edit Display ID"
          >
            <i class="bi bi-pencil"></i>
          </button>
          <div v-if="isEditingDisplayId" class="d-flex gap-1">
            <button class="btn btn-sm btn-success p-0 px-1" @click="saveDisplayId" title="Save">
              <i class="bi bi-check-lg"></i>
            </button>
            <button
              class="btn btn-sm btn-secondary p-0 px-1"
              @click="cancelEditDisplayId"
              title="Cancel"
            >
              <i class="bi bi-x-lg"></i>
            </button>
          </div>
        </div>
      </li>
      <li class="list-group-item p-2">
        <div class="fw-bold" :style="{ color: uiConfiguration.primaryTextColor }">UUID:</div>
        <span :style="{ color: uiConfiguration.secondaryTextColor }">
          {{ negotiation ? negotiation.id : '' }}</span
        >
      </li>
      <li class="list-group-item p-2">
        <div class="fw-bold" :style="{ color: uiConfiguration.primaryTextColor }">
          Submitted at:
        </div>
        <TimeStamp :value="negotiation ? negotiation.creationDate : ''" />
      </li>
      <li class="list-group-item p-2 d-flex justify-content-between">
        <div>
          <div class="fw-bold" :style="{ color: uiConfiguration.primaryTextColor }">Status:</div>
          <UiBadge
            :class="getBadgeColor(negotiation.status) + ' py-2 rounded-pill bg'"
            :icon="getBadgeIcon(negotiation.status)"
          >
            {{ negotiation ? transformStatus(negotiation.status) : '' }}
          </UiBadge>
        </div>
      </li>
      <li
        v-if="negotiation.status !== 'DRAFT' && possibleEvents.length > 0"
        class="list-group-item p-2 d-flex justify-content-between"
      >
        <ul class="list-unstyled mt-1 d-flex flex-row flex-wrap">
          <li v-for="event in possibleEvents" :key="event.label" class="me-2">
            <PrimaryButton
              :backgroundColor="getButtonColor(event.label)"
              :textColor="'#FFFFFF'"
              :size="'sm'"
              class="mb-1 d-flex text-left w-100"
              data-bs-toggle="modal"
              data-bs-target="#abandonModal"
              @click="assignStatus(event)"
            >
              <i :class="getButtonIcon(event.label)" />
              {{ event.label }}
            </PrimaryButton>
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
        <PDFButton
          id="pdf-button"
          class="mt-2"
          :negotiation-pdf-data="negotiation"
          data-cy="pdf-button"
          text="Download PDF"
          :include-attachments="false"
        />
        <PDFButton
          id="merged-pdf-button"
          class="mt-2"
          :negotiation-pdf-data="negotiation"
          data-cy="merged-pdf-button"
          text="Download PDF with attachments"
          badge-text="Beta"
          badge-type="warning"
          :include-attachments="true"
        />
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
import { ref } from 'vue'
import axios from 'axios'
import UiBadge from '@/components/ui/UiBadge.vue'
import PDFButton from '@/components/PDFButton.vue'
import TransferButton from '@/components/TransferButton.vue'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import {
  getBadgeColor,
  getBadgeIcon,
  getButtonColor,
  getButtonIcon,
  transformStatus,
} from '../composables/utils.js'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from '../store/notifications'
import TimeStamp from '@/components/ui/TimeStamp.vue'
import PrimaryButton from '@/components/ui/buttons/PrimaryButton.vue'

useNegotiationPageStore()
const notifications = useNotificationsStore()

const isEditingDisplayId = ref(false)
const editedDisplayId = ref('')

const props = defineProps({
  negotiation: { type: Object, required: true },
  author: { type: Object, required: true },
  possibleEvents: { type: Array, required: true },
  uiConfiguration: { type: Object, required: true },
  canDelete: { type: Function, required: true },
  isAdmin: { type: Boolean, default: false },
})

const emit = defineEmits([
  'assign-status',
  'download-attachment-from-link',
  'transfer-negotiation',
  'update-display-id',
])

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

function startEditDisplayId() {
  editedDisplayId.value = props.negotiation.displayId
  isEditingDisplayId.value = true
}

function cancelEditDisplayId() {
  isEditingDisplayId.value = false
  editedDisplayId.value = ''
}

async function saveDisplayId() {
  if (!editedDisplayId.value || editedDisplayId.value === props.negotiation.displayId) {
    cancelEditDisplayId()
    return
  }

  try {
    await axios.patch(
      `${apiPaths.BASE_API_PATH}/negotiations/${props.negotiation.id}`,
      { displayId: editedDisplayId.value },
      { headers: getBearerHeaders() },
    )

    // Emit event to update the negotiation in the parent component
    emit('update-display-id', editedDisplayId.value)

    notifications.setNotification('Display ID updated successfully')
    isEditingDisplayId.value = false
    editedDisplayId.value = ''
  } catch (error) {
    notifications.setNotification(
      'Error updating Display ID: ' + (error.response?.data?.message || error.message),
    )
  }
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
