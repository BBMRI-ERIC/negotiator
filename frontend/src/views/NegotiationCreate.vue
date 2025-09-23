<template>
  <div class="submit-modal">
    <button ref="openSaveModal" hidden data-bs-toggle="modal" data-bs-target="#feedbackModal" />
    <ConfirmationModal
      id="feedbackModal"
      :title="'Confirm submission'"
      :text="'You will be redirected to the negotiation page where you can monitor the status. Click Confirm to proceed.'"
      :message-enabled="false"
      dismiss-button-text="Back to HomePage"
      @confirm="updateSaveNegotiation(false)"
    />
  </div>
  <div class="negotiation-create-page">
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
        />
        <AccessFormOverview
          v-else-if="activeNavItemIndex == returnNavItems?.length + 1"
          :accessFormWithPayload="accessFormWithPayload"
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
            v-model:negotiationReplacedAttachmentsID="negotiationReplacedAttachmentsID"
            @element-focus-out-event="updateSaveNegotiation(true)"
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
          @openSaveNegotiationModal="openSaveNegotiationModal()"
          @saveDraft="openSaveNegotiationModal(true)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useUserStore } from '../store/user.js'
import FormNavigation from '../components/form-components/FormNavigation.vue'
import RequestSummary from '../components/form-components/RequestSummary.vue'
import AccessFormSection from '../components/form-components/AccessFormSection.vue'
import AccessFormOverview from '../components/form-components/AccessFormOverview.vue'
import FormNavigationButtons from '../components/form-components/FormNavigationButtons.vue'
import { useNegotiationFormStore } from '../store/negotiationForm.js'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import { useNotificationsStore } from '../store/notifications'
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

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }

  activeNavItemIndex.value = Number.isInteger(parseInt(route.query.step))
    ? parseInt(route.query.step)
    : 0

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
})

const existingAttachments = ref({})

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
      router.replace({ ...router.currentRoute, query: { step: newValue } })
    }

    if (oldValue > 0 && oldValue <= returnNavItems.value?.length) {
      validateInput(oldValue)
    }

    if (
      !props.isEditForm &&
      newValue !== oldValue &&
      newValue > 0 &&
      newValue <= returnNavItems.value?.length
    ) {
      createNegotiation()
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
        notificationsStore.setNotification(
          `Negotiation saved correctly as draft with id ${negotiationId}`,
          'light',
        )
        if (activeNavItemIndex.value) {
          router.push(`/edit/requests1/${negotiationId}?step=${activeNavItemIndex.value}`, {
            query: { step: activeNavItemIndex.value },
          })
        } else {
          router.push(`/edit/requests1/${negotiationId}`)
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
          notificationsStore.setNotification('Negotiation saved correctly as draft', 'light')
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
    negotiationFormStore.deleteAttachment(id)
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
