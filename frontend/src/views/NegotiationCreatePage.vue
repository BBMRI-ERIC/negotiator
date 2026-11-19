<template>
  <div class="submit-modal">
    <button ref="openSaveModal" hidden data-bs-toggle="modal" data-bs-target="#feedbackModal" />
    <button
      ref="openDeleteDraftModal"
      hidden
      data-bs-toggle="modal"
      data-bs-target="#deleteDraftModal"
    />
    <ConfirmationModal
      id="feedbackModal"
      :title="isDraftStatus ? 'Confirm submission' : 'Confirm changes'"
      :text="
        isDraftStatus
          ? 'You will be redirected to the negotiation page where you can monitor the status. Click Confirm to proceed.'
          : 'Your changes will be saved and you will be redirected to the negotiation page. Click Confirm to proceed.'
      "
      :message-enabled="false"
      dismiss-button-text="Back to HomePage"
      @confirm="updateSaveNegotiation(false)"
    />
    <ConfirmationModal
      id="deleteDraftModal"
      title="Delete Draft"
      text="Are you sure you want to delete this draft? All your data will be lost."
      :message-enabled="false"
      @confirm="deleteDraft"
    />
  </div>
  <div class="negotiation-create-page">
    <div class="d-flex flex-column flex-md-row mt-5">
      <div v-if="isLoading" class="loading-container">
        <div class="spinner-border loading-spinner" role="status" :style="{ color: spinnerColor }">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="mt-4 loading-text">Loading your request...</p>
      </div>
      <template v-else>
        <FormNavigation
          :navItems="returnNavItems"
          v-model:activeNavItemIndex="activeNavItemIndex"
          :validationErrorHighlight="validationErrorHighlight"
        />
        <div class="access-form d-flex flex-column align-middle mx-auto">
          <RequestSummary
            v-if="requestSummary && activeNavItemIndex === 0"
            :requestSummary="requestSummary"
            :negotiationId="requestId"
            :createdToday="isNegotiationCreatedToday"
            @resource-removed="handleResourceRemoved"
          />
          <AccessFormOverview
            v-else-if="activeNavItemIndex == returnNavItems?.length + 1"
            :accessFormWithPayload="accessFormWithPayload"
            :isDraftStatus="isDraftStatus"
            @emitErrorElementIndex="showSectionAndScrollToElement"
          />
          <div v-else>
            <AccessFormSection
              v-if="accessFormWithPayload"
              :validationErrorHighlight="
                validationErrorHighlight[
                  accessFormWithPayload.sections[activeNavItemIndex - 1].name
                ]
              "
              v-model:accessFormWithPayloadSection="
                accessFormWithPayload.sections[activeNavItemIndex - 1]
              "
              :focusElementId="focusElementId"
              v-model:negotiationReplacedAttachmentsID="negotiationReplacedAttachmentsID"
              @element-focus-out-event="isDraftStatus ? updateSaveNegotiation(true) : undefined"
              @element-focus-out-event-validation="validateInput"
              @element-focus-in-event="saveDraftDisabled = false"
            />
          </div>
          <FormNavigationButtons
            v-model:activeNavItemIndex="activeNavItemIndex"
            :navItemsLength="returnNavItems?.length"
            :requestId="requestId"
            :validationErrorHighlight="validationErrorHighlight"
            :saveDraftDisabled="saveDraftDisabled"
            :isDraftStatus="isDraftStatus"
            @openSaveNegotiationModal="openSaveNegotiationModal()"
            @saveDraft="openSaveNegotiationModal(true)"
            @deleteDraft="openDeleteDraftModalHandler()"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useUserStore } from '../store/user.js'
import FormNavigation from '../components/form-components/FormNavigation.vue'
import RequestSummary from '../components/form-components/RequestSummary.vue'
import AccessFormSection from '../components/form-components/AccessFormSection.vue'
import AccessFormOverview from '../components/form-components/AccessFormOverview.vue'
import FormNavigationButtons from '../components/form-components/FormNavigationButtons.vue'
import { useNegotiationFormStore } from '../store/negotiationForm.js'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import { useNegotiationsStore } from '../store/negotiations.js'
import { useNotificationsStore } from '../store/notifications.js'
import ConfirmationModal from '../components/modals/ConfirmationModal.vue'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({
  isEditForm: {
    type: Boolean,
    default: false,
  },
})

const router = useRouter()
const userStore = useUserStore()
const notificationsStore = useNotificationsStore()
const negotiationFormStore = useNegotiationFormStore()
const negotiationPageStore = useNegotiationPageStore()
const negotiationsStore = useNegotiationsStore()

const route = useRoute()
const uiConfigurationStore = useUiConfiguration()
const spinnerColor = computed(
  () => uiConfigurationStore.uiConfiguration?.theme?.primaryColor || '#26336B',
)
const requestId = ref(route.params.requestId)

const requestSummary = ref(null)
const activeNavItemIndex = ref(0)
const accessFormResponse = ref(null)
const accessFormWithPayload = ref(null)
const validationErrorHighlight = ref({})
const currentStatus = ref('')
const openSaveModal = ref(null)
const negotiationReplacedAttachmentsID = ref([])
const saveDraftDisabled = ref(true)
const recentDraftNegotiation = ref(null)
const isLoading = ref(true)

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }

  activeNavItemIndex.value = Number.isInteger(parseInt(route.query.step))
    ? parseInt(route.query.step)
    : 0
  await fetchRecentDraftNegotiation()

  if (props.isEditForm) {
    requestSummary.value = await negotiationPageStore.retrieveNegotiationById(requestId.value)
    requestSummary.value.resources =
      (await negotiationPageStore.retrieveResourcesByNegotiationId(requestId.value)) || []
    accessFormResponse.value = await negotiationFormStore.retrieveNegotiationCombinedAccessForm(
      requestId.value,
    )
    currentStatus.value = requestSummary.value.status
  } else {
    requestSummary.value = await negotiationFormStore.retrieveRequestById(requestId.value)
    accessFormResponse.value = await negotiationFormStore.retrieveCombinedAccessForm(
      requestId.value,
    )
  }

  if (requestSummary.value.code) {
    if (requestSummary.value.code === 404) {
      notificationsStore.setNotification(
        `${props.isEditForm ? 'Request' : 'Negotiation'} not found`,
        'danger',
      )
    } else {
      notificationsStore.setNotification(
        `Cannot contact the server to get ${props.isEditForm ? 'request' : 'negotiation'} information`,
        'danger',
      )
    }
  }

  accessFormWithPayload.value = createAccessFormWithPayload()
  validateAllInputs()

  if (!props.isEditForm) {
    await createNegotiation()
  }
  if (recentDraftNegotiation.value) {
    handleMergeWithDraft(recentDraftNegotiation.value)
  }
  isLoading.value = false
})

const isDraftStatus = computed(() => {
  return currentStatus.value === 'DRAFT'
})

const isNegotiationCreatedToday = computed(() => {
  const creationDate = requestSummary.value?.creationDate
  if (!creationDate) return false
  const created = new Date(creationDate)
  const today = new Date()
  return (
    created.getFullYear() === today.getFullYear() &&
    created.getMonth() === today.getMonth() &&
    created.getDate() === today.getDate()
  )
})

function createAccessFormWithPayload() {
  const payload = accessFormResponse.value

  payload.sections.forEach((section) => {
    section.elements.forEach((element) => {
      switch (element.type) {
        case 'TEXT':
        case 'TEXT_LARGE':
          element.value = requestSummary.value.payload
            ? requestSummary.value.payload[section.name][element.name]
            : ''
          break
        case 'NUMBER':
          element.value = requestSummary.value.payload
            ? requestSummary.value.payload[section.name][element.name]
            : ''
          break
        case 'BOOLEAN':
          element.value = requestSummary.value.payload
            ? requestSummary.value.payload[section.name][element.name]
            : ''
          break
        case 'SINGLE_CHOICE_DROPDOWN':
          element.value = requestSummary.value.payload?.[section.name]?.[element.name] ?? ''
          break
        case 'MULTIPLE_CHOICE':
        case 'SINGLE_CHOICE_RADIO':
          element.value = requestSummary.value.payload
            ? requestSummary.value.payload[section.name][element.name]
            : []
          break
        case 'FILE':
          element.value = requestSummary.value.payload
            ? requestSummary.value.payload[section.name][element.name]
            : null
          break
        default:
          element.value = requestSummary.value.payload
            ? requestSummary.value.payload[section.name][element.name]
            : null // Default fallback
      }
    })
  })

  return payload
}

const returnNavItems = computed(() => {
  return accessFormResponse.value?.sections.map((section, index) => {
    return {
      id: index + 1,
      name: section.name,
      label: section.label,
      description: section.description,
    }
  })
})

// Built fresh for every save: a cached copy would keep values the save replaced.
function buildPayload() {
  const result = {}
  accessFormWithPayload.value.sections.forEach((section) => {
    result[section.name] = {}
    section.elements.forEach((element) => {
      result[section.name][element.name] = element.value
    })
  })
  return result
}

watch(
  () => activeNavItemIndex.value,
  (newValue, oldValue) => {
    if (Number.isInteger(newValue)) {
      router.replace({ query: { step: newValue } })
    }

    if (oldValue > 0 && oldValue <= returnNavItems.value?.length) {
      validateInput(oldValue)
    }
  },
  { deep: true },
)

function validateInput(activeIndex) {
  if (!activeIndex) {
    activeIndex = activeNavItemIndex.value
  }
  validationErrorHighlight.value[accessFormWithPayload.value.sections[activeIndex - 1].name] =
    accessFormWithPayload.value.sections[activeIndex - 1].elements
      .filter(
        (element) => element.required === true && checkIfElementValueValid(element.value) === false,
      )
      .map((element) => element.id)
}

function validateAllInputs() {
  accessFormWithPayload.value.sections.forEach((section, index) => {
    validateInput(index + 1)
  })
}

function checkIfElementValueValid(elementValue) {
  return !(
    elementValue === '' ||
    elementValue === null ||
    (Array.isArray(elementValue) && elementValue.length === 0)
  )
}

async function createNegotiation() {
  // Implementation for creating a negotiation
  const data = {
    draft: true,
    request: requestId.value,
    payload: buildPayload(),
  }
  await negotiationFormStore
    .createNegotiation(data)
    .then((negotiationId) => {
      if (negotiationId) {
        if (activeNavItemIndex.value) {
          router.push({
            path: `/edit/requests/${negotiationId}`,
            query: { step: activeNavItemIndex.value },
          })
        } else {
          router.push(`/edit/requests/${negotiationId}`)
        }
      }
    })
    .catch(() => {
      notificationsStore.setNotification('Failed save changes', 'danger')
    })
}

async function updateSaveNegotiation(isSavingDraft) {
  saveDraftDisabled.value = true
  if (!props.isEditForm) return
  if (!(await uploadPendingFiles())) return
  const payload = buildPayload()
  // A file picked while the uploads ran is left to the save that its own focus-out starts.
  if (hasPendingFiles()) return
  // Only attachments replaced before this payload was built are no longer referenced by it.
  const replacedIds = [...negotiationReplacedAttachmentsID.value]
  const updated = await negotiationFormStore.updateNegotiationById(requestId.value, { payload })
  if (updated === null) return
  await deleteReplacedAttachments(replacedIds)

  if (!isSavingDraft) {
    if (currentStatus.value === 'DRAFT') {
      negotiationPageStore.updateNegotiationStatus(requestId.value, 'SUBMIT').then(() => {
        backToNegotiation(requestId.value)
      })
    } else {
      backToNegotiation(requestId.value)
    }
  } else {
    notificationsStore.setNotification(
      isDraftStatus.value
        ? 'Negotiation saved correctly as draft'
        : 'Negotiation changes saved successfully',
      'light',
    )
  }
}

// Saves overlap (every focus-out of a draft starts one), so a save that finds an
// upload of the same File already running waits for it instead of uploading again.
const pendingUploads = new WeakMap()

function uploadOnce(file) {
  let upload = pendingUploads.get(file)
  if (!upload) {
    upload = negotiationFormStore.uploadAttachment(requestId.value, file).then((attachment) => {
      if (!attachment) pendingUploads.delete(file)
      return attachment
    })
    pendingUploads.set(file, upload)
  }
  return upload
}

function fileElementsToUpload() {
  return accessFormWithPayload.value.sections
    .flatMap((section) => section.elements)
    .filter((element) => element.value instanceof File)
}

function hasPendingFiles() {
  return fileElementsToUpload().length > 0
}

// Uploads every picked File and puts the returned attachment in its place.
// Returns false if any upload failed.
async function uploadPendingFiles() {
  const results = await Promise.all(
    fileElementsToUpload().map(async (element) => {
      const file = element.value
      const attachment = await uploadOnce(file)
      if (!attachment) return false
      if (element.value === file) {
        element.value = attachment
      } else if (
        element.value?.id !== attachment.id &&
        !negotiationReplacedAttachmentsID.value.includes(attachment.id)
      ) {
        // Another file was picked while this one was uploading.
        negotiationReplacedAttachmentsID.value.push(attachment.id)
      }
      return true
    }),
  )
  return results.every(Boolean)
}

// Takes the ids out of the list first so overlapping saves never delete one twice.
async function deleteReplacedAttachments(replacedIds) {
  const pending = negotiationReplacedAttachmentsID.value
  const ids = replacedIds.filter((id) => pending.includes(id))
  ids.forEach((id) => pending.splice(pending.indexOf(id), 1))
  const results = await Promise.all(ids.map((id) => negotiationFormStore.deleteAttachment(id)))
  ids.forEach((id, index) => {
    if (results[index] === null) negotiationReplacedAttachmentsID.value.push(id)
  })
}

function backToNegotiation(id) {
  router.push('/negotiations/' + id + '/ROLE_RESEARCHER')
}

function openSaveNegotiationModal() {
  validateAllInputs()

  if (Object.values(validationErrorHighlight.value).some((errors) => errors.length > 0)) {
    notificationsStore.setNotification('Please fill in all required fields', 'danger')
    return
  }
  openSaveModal.value.click()
}

const openDeleteDraftModal = ref(null)

function openDeleteDraftModalHandler() {
  openDeleteDraftModal.value.click()
}

async function deleteDraft() {
  try {
    await negotiationPageStore.deleteNegotiation(requestId.value)
    notificationsStore.setNotification('Draft deleted successfully', 'success')
    router.push('/')
  } catch {
    notificationsStore.setNotification('Failed to delete draft', 'danger')
  }
}

async function handleResourceRemoved() {
  try {
    requestSummary.value.resources = await negotiationPageStore.retrieveResourcesByNegotiationId(
      requestId.value,
    )
  } catch (error) {
    console.error('Error reloading request summary:', error)
  }
}

const focusElementId = ref(null)

function showSectionAndScrollToElement(item) {
  focusElementId.value = item.elementId
  activeNavItemIndex.value = item.sectionIndex + 1
  const calcYOffset = item.elementIndex * 70 + 200
  validateInput()
  if (calcYOffset) {
    window.scroll(0, calcYOffset)
    return
  }
}

async function fetchRecentDraftNegotiation() {
  try {
    const filtersSortData = {
      status: ['DRAFT'],
    }

    const response = await negotiationsStore.retrieveNegotiationsByUserId(
      'author',
      filtersSortData,
      userStore.userInfo.id,
      0,
    )

    if (response?._embedded?.negotiations?.length > 0) {
      // Get the most recent draft (first in the sorted list)
      const mostRecentDraft = response._embedded.negotiations[1]
      const createdDate = new Date(mostRecentDraft.creationDate)
      const today = new Date()
      const isToday =
        createdDate.getFullYear() === today.getFullYear() &&
        createdDate.getMonth() === today.getMonth() &&
        createdDate.getDate() === today.getDate()
      if (isToday) {
        recentDraftNegotiation.value = mostRecentDraft
      }
    }
  } catch (error) {
    console.error('Error fetching recent draft negotiations:', error)
  }
}

async function handleMergeWithDraft(draftNegotiation) {
  try {
    // Get resource IDs from the current request
    const resourceIds = requestSummary.value.resources?.map((resource) => resource.id) || []

    if (resourceIds.length === 0) {
      notificationsStore.setNotification('No resources found to merge', 'warning')
      return
    }

    // Call the API to add resources to the draft negotiation
    const result = await negotiationPageStore.addResources(
      { resourceIds },
      draftNegotiation.id,
      true,
    )

    // Check if the API call was successful (returned data)
    if (result) {
      await negotiationPageStore.deleteNegotiation(requestId.value, true)
      router.push(`/edit/requests/${draftNegotiation.id}`)
    }
  } catch (error) {
    console.error('Error merging with draft negotiation:', error)
    notificationsStore.setNotification('Failed to merge resources with draft negotiation', 'danger')
  }
}
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 60vh;
}

.loading-spinner {
  width: 4rem;
  height: 4rem;
  border-width: 0.35rem;
  color: #26336b;
}

.loading-text {
  font-size: 1.1rem;
  font-weight: 500;
  color: #6c757d;
  letter-spacing: 0.02em;
}

.access-form {
  width: 50%;
}

@media screen and (max-width: 480px) {
  .access-form {
    width: 100%;
  }
}
</style>
