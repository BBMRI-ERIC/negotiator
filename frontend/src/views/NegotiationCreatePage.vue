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
    <DraftNegotiationMergeBanner
      :draftNegotiation="recentDraftNegotiation"
      @merge="handleMergeWithDraft"
      @dismiss="handleDismissBanner"
    />
    <div class="d-flex flex-column flex-md-row mt-5">
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
              validationErrorHighlight[accessFormWithPayload.sections[activeNavItemIndex - 1].name]
            "
            :existingAttachments="
              existingAttachments[accessFormWithPayload.sections[activeNavItemIndex - 1].name]
            "
            :negotiationAttachments="negotiationAttachments"
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
import DraftNegotiationMergeBanner from '../components/DraftNegotiationMergeBanner.vue'
import { useNegotiationFormStore } from '../store/negotiationForm.js'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import { useNegotiationsStore } from '../store/negotiations.js'
import { useNotificationsStore } from '../store/notifications.js'
import ConfirmationModal from '../components/modals/ConfirmationModal.vue'

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
const requestId = ref(route.params.requestId)

const requestSummary = ref(null)
const activeNavItemIndex = ref(0)
const accessFormResponse = ref(null)
const accessFormWithPayload = ref(null)
const validationErrorHighlight = ref({})
const currentStatus = ref('')
const negotiationAttachments = ref([])
const openSaveModal = ref(null)
const negotiationReplacedAttachmentsID = ref([])
const saveDraftDisabled = ref(true)
const recentDraftNegotiation = ref(null)

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
    negotiationAttachments.value = await negotiationPageStore.retrieveAttachmentsByNegotiationId(
      requestId.value,
    )
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
  if (recentDraftNegotiation.value){
    handleMergeWithDraft(recentDraftNegotiation.value)
  }
})

const existingAttachments = ref({})

const isDraftStatus = computed(() => {
  return currentStatus.value === 'DRAFT'
})

function createAccessFormWithPayload() {
  const payload = accessFormResponse.value

  payload.sections.forEach((section) => {
    existingAttachments.value[section.name] = []
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
        case 'MULTIPLE_CHOICE':
        case 'SINGLE_CHOICE':
          element.value = requestSummary.value.payload
            ? requestSummary.value.payload[section.name][element.name]
            : []
          break
        case 'FILE':
          if (requestSummary.value?.payload)
            existingAttachments.value[section.name].push(
              requestSummary.value.payload[section.name][element.name],
            )
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

const mappedAccessForm = computed(() => {
  const result = {}
  accessFormWithPayload.value.sections.forEach((section) => {
    result[section.name] = {}
    section.elements.forEach((element) => {
      result[section.name][element.name] = element.value
    })
  })
  return result
})

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
    payload: mappedAccessForm.value,
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
  if (props.isEditForm) {
    const data = {
      payload: mappedAccessForm.value,
    }
    const disableAttachmentUpload =
      negotiationReplacedAttachmentsID.value.length === 0 ? true : false
    await negotiationFormStore
      .updateNegotiationById(requestId.value, data, disableAttachmentUpload)
      .then(() => {
        deleteAllReplacedAttachments()
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
      })
  }
}

function backToNegotiation(id) {
  router.push('/negotiations/' + id + '/ROLE_RESEARCHER')
}

function deleteAllReplacedAttachments() {
  negotiationReplacedAttachmentsID.value.forEach((id) => {
    if (id === null) return
    negotiationFormStore.deleteAttachment(id).then(() => {
      negotiationReplacedAttachmentsID.value = negotiationReplacedAttachmentsID.value.filter(
        (attachmentId) => attachmentId !== id,
      )
    })
  })
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
      recentDraftNegotiation.value = mostRecentDraft
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
    const result = await negotiationPageStore.addResources({ resourceIds }, draftNegotiation.id)

    // Check if the API call was successful (returned data)
    if (result) {
      await negotiationPageStore.deleteNegotiation(requestId.value)
      router.push(`/edit/requests/${draftNegotiation.id}`)
      notificationsStore.setNotification(
        'Resources added',
        'success',
      )
    }
  } catch (error) {
    console.error('Error merging with draft negotiation:', error)
    notificationsStore.setNotification('Failed to merge resources with draft negotiation', 'danger')
  }
}

function handleDismissBanner() {
  // Simply hide the banner by clearing the reference
  recentDraftNegotiation.value = null
}
</script>

<style scoped>
.access-form {
  width: 50%;
}

@media screen and (max-width: 480px) {
  .access-form {
    width: 100%;
  }
}
</style>
