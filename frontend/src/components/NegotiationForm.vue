<template>
  <button ref="openModal" hidden data-bs-toggle="modal" data-bs-target="#feedbackModal" />
  <confirmation-modal
    id="feedbackModal"
    :title="notificationTitle"
    :text="notificationText"
    :message-enabled="false"
    dismiss-button-text="Back to HomePage"
    @dismiss="backToHomePage"
    @confirm="startNegotiation"
  />
  <div v-if="loading" class="d-flex align-items-center justify-content-center">
    <h4 class="me-2">Loading...</h4>
    <div class="spinner-border" role="status" />
  </div>
  <div v-else>
    <div
      v-if="isEditForm"
      class="fs-3 mb-4 fw-bold text-center"
      :style="{ color: uiConfiguration?.primaryTextColor }"
    >
      Edit Negotiation Form
    </div>
    <div
      v-else
      class="fs-3 mb-4 fw-bold text-center"
      :style="{ color: uiConfiguration?.primaryTextColor }"
    >
      Access Form Submission
    </div>
    <form-wizard
      v-if="accessForm"
      :start-index="0"
      :color="uiConfiguration?.primaryTextColor"
      step-size="md"
      @on-complete="startModal(false)"
    >
      <tab-content
        title="Request summary"
        class="form-step border rounded-2 px-2 py-3 mb-2 overflow-auto"
      >
        <div class="mx-3">
          <div class="fs-5 fw-bold" :style="{ color: uiConfiguration?.primaryTextColor }">
            SEARCH PARAMETERS
          </div>
          <div
            v-for="(qp, index) in queryParameters"
            :key="index"
            class="fs-6 text-dar"
            :style="{ color: uiConfiguration?.secondaryTextColor }"
          >
            {{ qp }}
          </div>
        </div>
        <hr class="mx-3" />
        <resources-list class="mx-3" :resources="resources" />
      </tab-content>
      <tab-content
        v-for="section in accessForm.sections"
        :key="section.name"
        :title="section.label"
        class="form-step border rounded-2 px-2 py-3 mb-2 overflow-auto"
        :style="{ color: uiConfiguration?.primaryTextColor }"
        :before-change="isSectionValid(section)"
      >
        <div v-if="section.description" class="mx-3 d-flex justify-content-end">
          <i
            class="py-1 bi bi-info-circle"
            data-bs-toggle="tooltip"
            :data-bs-title="section.description"
            :style="{ color: uiConfiguration?.primaryTextColor }"
          />
        </div>

        <div v-for="criteria in section.elements" :key="criteria.name" class="mb-4 mx-3">
          <label
            class="form-label"
            :style="{ color: uiConfiguration?.primaryTextColor }"
            :class="{ required: criteria.required }"
          >
            {{ criteria.label }}
          </label>

          <span v-if="criteria.description" class="ms-2 text-muted">
            <i
              class="py-1 bi bi-info-circle"
              data-bs-toggle="tooltip"
              :data-bs-title="criteria.description"
              :style="{ color: uiConfiguration?.primaryTextColor }"
            />
          </span>

          <div v-if="criteria.type === 'TEXT'">
            <input
              v-model="negotiationCriteria[section.name][criteria.name]"
              :type="criteria.type"
              :placeholder="criteria.description"
              class="form-control text-secondary-text"
              :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
              :required="criteria.required"
            />
          </div>

          <div v-else-if="criteria.type === 'BOOLEAN'">
            <div class="form-check form-check-inline">
              <input
                id="inlineRadio1"
                v-model="negotiationCriteria[section.name][criteria.name]"
                value="Yes"
                :required="criteria.required"
                class="form-check-input"
                :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
                type="radio"
              />
              <label class="form-check-label" for="inlineRadio1"> Yes </label>
            </div>
            <div class="form-check form-check-inline">
              <input
                id="inlineRadio2"
                v-model="negotiationCriteria[section.name][criteria.name]"
                value="No"
                :required="criteria.required"
                class="form-check-input"
                :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
                type="radio"
              />
              <label class="form-check-label" for="inlineRadio2"> No </label>
            </div>
          </div>

          <div v-else-if="criteria.type === 'MULTIPLE_CHOICE'">
            <div
              v-for="(value, index) in negotiationValueSets[criteria.id]?.availableValues"
              :key="index"
            >
              <div class="form-check form-check-inline">
                <input
                  id="inlineCheckbox1"
                  v-model="negotiationCriteria[section.name][criteria.name]"
                  :value="value"
                  :required="criteria.required"
                  class="form-check-input"
                  :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
                  type="checkbox"
                />
                <label class="form-check-label" for="inlineCheckbox1">{{ value }}</label>
              </div>
            </div>
            <div
              v-if="
                negotiationValueSets[criteria.id]?.externalDocumentation &&
                negotiationValueSets[criteria.id]?.externalDocumentation !== 'none'
              "
            >
              <span class="text-muted"> External Documentation - </span>
              <a
                :href="negotiationValueSets[criteria.id]?.externalDocumentation"
                :style="{ color: uiConfiguration?.linksTextColor }"
              >
                {{ negotiationValueSets[criteria.id]?.externalDocumentation }}
              </a>
            </div>
          </div>

          <div v-else-if="criteria.type === 'SINGLE_CHOICE'">
            <div
              v-for="(value, index) in negotiationValueSets[criteria.id]?.availableValues"
              :key="index"
            >
              <div class="form-check form-check-inline">
                <input
                  id="inlineRadio1"
                  v-model="negotiationCriteria[section.name][criteria.name]"
                  :value="value"
                  :required="criteria.required"
                  class="form-check-input"
                  :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
                  type="radio"
                  @click="uncheckRadioButton(value, section.name, criteria.name)"
                />
                <label class="form-check-label" for="inlineRadio1">{{ value }}</label>
              </div>
            </div>
          </div>

          <div v-else-if="criteria.type === 'TEXT_LARGE'">
            <textarea
              v-model="negotiationCriteria[section.name][criteria.name]"
              :placeholder="criteria.description"
              class="form-control text-secondary-text"
              :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
              :required="criteria.required"
            />
          </div>

          <div v-else-if="criteria.type === 'NUMBER'" class="col-5">
            <input
              v-model="negotiationCriteria[section.name][criteria.name]"
              :type="criteria.type"
              :placeholder="criteria.description"
              class="form-control text-secondary-text"
              :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
              :required="criteria.required"
              @keypress="isNumber($event)"
            />
          </div>

          <div v-else-if="criteria.type === 'FILE'">
            <label v-if="isEditForm" class="form-label text-primary-text">
              Uploaded file: {{ negotiationCriteria[section.name][criteria.name].name }}
            </label>
            <input
              accept=".pdf"
              class="form-control text-secondary-text"
              :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
              :required="criteria.required"
              :placeholder="criteria.description"
              :type="criteria.type"
              @change="handleFileUpload($event, section.name, criteria.name)"
            />
          </div>

          <div v-else-if="criteria.type === 'DATE'" class="w-25">
            <p v-if="criteria.description" class="text-muted">
              {{ criteria.description }}
            </p>
            <input
              id="startDate"
              v-model="negotiationCriteria[section.name][criteria.name]"
              value=""
              class="form-control form-control-sm"
              :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
              type="date"
            />
          </div>

          <div v-else-if="criteria.type === 'INFORMATION'">
            <p v-if="criteria.description" class="text-muted">
              {{ criteria.description }}
            </p>
          </div>

          <input
            v-else
            v-model="negotiationCriteria[section.name][criteria.name]"
            :type="criteria.type"
            :placeholder="criteria.description"
            class="form-control text-secondary-text"
            :required="criteria.required"
          />

          <div v-if="validationColorHighlight.includes(criteria.name)" class="invalidText">
            {{ transformMessage(criteria.type) }}
          </div>
          <div
            v-if="
              negotiationValueSets[criteria.id]?.externalDocumentation &&
              negotiationValueSets[criteria.id]?.externalDocumentation !== 'none'
            "
            class="mt-2"
          >
            <span class="text-muted"> External Documentation - </span>
            <a
              :href="negotiationValueSets[criteria.id]?.externalDocumentation"
              :style="{ color: uiConfiguration?.linksTextColor }"
            >
              {{ negotiationValueSets[criteria.id]?.externalDocumentation }}
            </a>
          </div>
        </div>
      </tab-content>
      <tab-content title="Overview" class="form-step overflow-auto">
        <div class="border rounded-2 input-group p-3 mb-2 mb-3">
          <span class="mb-3 fs-4 fw-bold" :style="{ color: uiConfiguration?.primaryTextColor }">
            Overview*
          </span>
          <span :style="{ color: uiConfiguration?.secondaryTextColor }"
            >Upon confirmation, your request will undergo content review. Our reviewers may contact
            you via email for further details. Upon approval, the respective biobanks you wish to
            contact will be notified of your request. Please click 'Submit request' and then
            'Confirm' to proceed.</span
          >
        </div>
        <div
          v-for="section in accessForm.sections"
          :key="section.name"
          class="border rounded-2 input-group p-3 mb-2 mb-3"
        >
          <span class="mb-3 fs-4 fw-bold" :style="{ color: uiConfiguration?.primaryTextColor }">{{
            section.label.toUpperCase()
          }}</span>
          <div
            v-for="accessFormElement in section.elements"
            :key="accessFormElement.name"
            class="input-group mb-2"
          >
            <label class="me-2 fw-bold" :style="{ color: uiConfiguration?.primaryTextColor }"
              >{{ accessFormElement.label }}:</label
            >
            <span
              v-if="isAttachment(negotiationCriteria[section.name][accessFormElement.name])"
              :style="{ color: uiConfiguration?.secondaryTextColor }"
            >
              <span v-if="negotiationCriteria[section.name][accessFormElement.name].name">{{
                negotiationCriteria[section.name][accessFormElement.name].name
              }}</span>
              <div
                v-for="(choice, index) in negotiationCriteria[section.name][accessFormElement.name]"
                v-else
                :key="index"
              >
                {{ choice }}
              </div>
            </span>
            <span v-else :style="{ color: uiConfiguration?.secondaryTextColor }">
              {{ translateTrueFalse(negotiationCriteria[section.name][accessFormElement.name]) }}
            </span>
          </div>
        </div>
      </tab-content>          
      <template #footer="props">
        <div class="wizard-footer-left">
          <button
            v-if="props.activeTabIndex > 0"
            type="button"
            class="btn"
            :style="{
              'background-color': uiConfiguration.buttonColor,
              'border-color': uiConfiguration.buttonColor,
              color: '#FFFFFF',
            }"
            @click="props.prevTab()"
          >
            Previous
          </button>
        </div>
        <div class="wizard-footer-right">
          <button
            v-if="props.activeTabIndex > 0"
            class="btn me-4"
            @click="startModal(true)"
            :style="{
              'background-color': uiConfiguration.buttonColor,
              'border-color': uiConfiguration.buttonColor,
              color: '#FFFFFF',
            }"
          >
            Save Draft
          </button>
          <button
            class="btn"
            @click="props.nextTab()"
            :style="{
              'background-color': uiConfiguration.buttonColor,
              'border-color': uiConfiguration.buttonTextColor,
              color: '#FFFFFF',
            }"
          >
            {{ props.isLastStep ? 'Submit request' : 'Next' }}
          </button>
        </div>
      </template>
    </form-wizard>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeMount, onMounted } from 'vue'
import { Tooltip } from 'bootstrap'
import { useRouter } from 'vue-router'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'
import ResourcesList from '@/components/ResourcesList.vue'
import { FormWizard, TabContent } from 'vue3-form-wizard'
import { useNegotiationFormStore } from '../store/negotiationForm'
import { useNotificationsStore } from '../store/notifications'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import 'vue3-form-wizard/dist/style.css'

const uiConfigurationStore = useUiConfiguration()
const negotiationFormStore = useNegotiationFormStore()
const notificationsStore = useNotificationsStore()
const negotiationPageStore = useNegotiationPageStore()
const router = useRouter()
const props = defineProps({
  requestId: {
    type: String,
    default: undefined,
  },
  isEditForm: {
    type: Boolean,
    default: false,
  },
})

const saveDraft = ref(false)
const notificationTitle = ref('')
const notificationText = ref('')
const negotiationCriteria = ref({})
const negotiationValueSets = ref({})
const validationColorHighlight = ref([])

const accessForm = ref(undefined)
const resources = ref([])
const humanReadableSearchParameters = ref([])
const openModal = ref(null)
const requestAlreadySubmittedNegotiationId = ref(null)

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

const loading = computed(() => {
  return accessForm.value === undefined
})

const queryParameters = computed(() => {
  return humanReadableSearchParameters.value.split('\r\n')
})

onBeforeMount(async () => {
  let result = {}
  let negotiation = {}
  if (props.isEditForm) {
    negotiation = await negotiationPageStore.retrieveNegotiationById(props.requestId)
  } else {
    result = await negotiationFormStore.retrieveRequestById(props.requestId)
  }

  if (result.code) {
    if (result.code === 404) {
      showNotification('Error', 'Request not found')
    } else {
      showNotification('Error', 'Cannot contact the server to get request information')
    }
  } else if (result.negotiationId) {
    requestAlreadySubmittedNegotiationId.value = result.negotiationId
    showNotification('Error', 'Request already submitted')
  } else {
    if (props.isEditForm) {
      accessForm.value = await negotiationFormStore.retrieveNegotiationCombinedAccessForm(
        props.requestId,
      )
      humanReadableSearchParameters.value = ''
    } else {
      accessForm.value = await negotiationFormStore.retrieveCombinedAccessForm(props.requestId)
      resources.value = result.resources
      humanReadableSearchParameters.value = result.humanReadable
    }
    if (accessForm.value !== undefined) {
      initNegotiationCriteria(negotiation?.payload)
    }
  }
})

onMounted(() => {
  new Tooltip(document.body, {
    selector: "[data-bs-toggle='tooltip']",
  })
})

function backToNegotiation(id) {
  router.push('/negotiations/' + id + '/ROLE_RESEARCHER')
}

async function getValueSet(id) {
  await negotiationFormStore.retrieveDynamicAccessFormsValueSetByID(id).then((res) => {
    negotiationValueSets.value[id] = res
  })
}

async function startNegotiation() {
  if (props.isEditForm) {
    const data = {
      payload: negotiationCriteria.value,
    }
    await negotiationFormStore.updateNegotiationById(props.requestId, data).then(() => {
      backToNegotiation(props.requestId)
    })
  } else {
    if (requestAlreadySubmittedNegotiationId.value) {
      backToNegotiation(requestAlreadySubmittedNegotiationId.value)
      return
    }
    const data = {
      draft: saveDraft.value,
      request: props.requestId,
      payload: negotiationCriteria.value,
    }
    await negotiationFormStore.createNegotiation(data).then((negotiationId) => {
      if (negotiationId) {
        backToNegotiation(negotiationId)
      }
    })
  }
}

function startModal(isDraft) {
  if (!isDraft) {
    showNotification(
      'Confirm submission',
      "You will be redirected to the negotiation page where you can monitor the status. Click 'Confirm' to proceed.",
    )
  } else {
    showNotification(
      'Confirm saving',
      "You are about to save the form as a draft. To complete the request you will find the negotiation in you negotiation list. Click 'Confirm' to proceed.",
    )
  }
  this.saveDraft = isDraft
}

function isAttachment(value) {
  return value instanceof File || value instanceof Object
}

function handleFileUpload(event, section, criteria) {
  negotiationCriteria.value[section][criteria] = event.target.files[0]
}

function showNotification(header, body) {
  openModal.value.click()
  notificationTitle.value = header
  notificationText.value = body
}

function initNegotiationCriteria(negotiationPayload) {
  if (negotiationPayload) {
    for (const section of accessForm.value.sections) {
      negotiationCriteria.value[section.name] = {}
      for (const criteria of section.elements) {
        if (criteria.type === 'MULTIPLE_CHOICE') {
          if (negotiationPayload[section.name][criteria.name]) {
            negotiationCriteria.value[section.name][criteria.name] =
              negotiationPayload[section.name][criteria.name]
          } else {
            negotiationCriteria.value[section.name][criteria.name] = []
          }
          getValueSet(criteria.id)
        } else if (criteria.type === 'SINGLE_CHOICE') {
          negotiationCriteria.value[section.name][criteria.name] =
            negotiationPayload[section.name][criteria.name]
          getValueSet(criteria.id)
        } else if (criteria.type === 'FILE') {
          if (negotiationPayload[section.name][criteria.name]) {
            negotiationCriteria.value[section.name][criteria.name] =
              negotiationPayload[section.name][criteria.name]
          } else {
            negotiationCriteria.value[section.name][criteria.name] = {}
          }
        } else {
          negotiationCriteria.value[section.name][criteria.name] =
            negotiationPayload[section.name][criteria.name]
        }
      }
    }
  } else {
    for (const section of accessForm.value.sections) {
      negotiationCriteria.value[section.name] = {}
      for (const criteria of section.elements) {
        if (criteria.type === 'MULTIPLE_CHOICE') {
          negotiationCriteria.value[section.name][criteria.name] = []
          getValueSet(criteria.id)
        } else if (criteria.type === 'SINGLE_CHOICE') {
          getValueSet(criteria.id)
        } else {
          negotiationCriteria.value[section.name][criteria.name] = null
        }
      }
    }
  }
}

function isSectionValid(section) {
  return () => {
    let valid = true
    validationColorHighlight.value = []
    section.elements.forEach((ac) => {
      if (ac.required) {
        if (
          ac.type === 'MULTIPLE_CHOICE' &&
          Object.keys(negotiationCriteria.value[section.name][ac.name]).length === 0
        ) {
          validationColorHighlight.value.push(ac.name)
          valid = false
        } else if (
          ac.type === 'FILE' &&
          (typeof negotiationCriteria.value[section.name][ac.name] !== 'object' ||
            negotiationCriteria.value[section.name][ac.name] === null)
        ) {
          validationColorHighlight.value.push(ac.name)
          valid = false
        } else if (
          ac.type !== 'MULTIPLE_CHOICE' &&
          ac.type !== 'FILE' &&
          (typeof negotiationCriteria.value[section.name][ac.name] !== 'string' ||
            negotiationCriteria.value[section.name][ac.name] === '')
        ) {
          validationColorHighlight.value.push(ac.name)
          valid = false
        }
      } else if (valid) {
        valid = true
      }
    })
    if (!valid) {
      notificationsStore.notification = 'Please fill out all required fields correctly'
    }
    return valid
  }
}

function uncheckRadioButton(value, sectionName, criteriaName) {
  if (negotiationCriteria.value[sectionName][criteriaName] === value) {
    negotiationCriteria.value[sectionName][criteriaName] = ''
  }
}

function translateTrueFalse(value) {
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }
  return value
}

function isNumber(evt) {
  const charCode = evt.which ? evt.which : evt.keyCode
  if (charCode > 31 && (charCode < 48 || charCode > 57) && charCode !== 46) {
    evt.preventDefault()
  }
}

function transformMessage(text) {
  if (text == 'SINGLE_CHOICE' || text == 'BOOLEAN') {
    return 'Please select one of available values'
  } else if (text == 'MULTIPLE_CHOICE') {
    return 'Please select at least one of the available values'
  } else if (text == 'TEXT_LARGE') {
    return 'Please provide a text'
  } else {
    return 'Please provide a ' + text?.toLowerCase()
  }
}
</script>

<style scoped>
.required:after {
  content: '  *\00a0';
  color: red;
}

.bi:hover {
  color: #7c7c7c;
}

.invalidText {
  width: 100%;
  margin-top: 0.25rem;
  font-size: 0.875em;
  color: var(--bs-form-invalid-color);
}
</style>
