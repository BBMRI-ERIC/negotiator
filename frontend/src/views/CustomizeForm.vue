<template>
  <div>
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
    <button
      ref="openAddFormSectionModal"
      hidden
      data-bs-toggle="modal"
      data-bs-target="#fromWizardTabModal"
    />

    <add-form-section-modal
      id="fromWizardTabModal"
      :title="'Add Section details'"
      :text="'Please input fields'"
      dismiss-button-text="Back to HomePage"
      @dismiss="backToHomePage"
      @confirm="addFormSection"
    />

    <button
      @click="openAddFormSectionModal.click()"
      type="button"
      class="btn btn-sm sm btn-danger my-3 py-1 px-1 me-md-5"
    >
      + Add form section
    </button>

    <form-wizard
      :key="forceReRenderFormWizard"
      v-if="accessForm"
      :start-index="0"
      :color="uiConfiguration?.primaryTextColor"
      step-size="md"
      @on-complete="startModal"
    >
      <tab-content
        v-for="(section, index) in accessForm.sections"
        :key="section.name"
        :title="section.label"
        class="form-step border rounded-2 px-2 py-3 mb-2 overflow-auto"
        :style="{ color: uiConfiguration?.primaryTextColor }"
        :before-change="isSectionValid(section)"
      >
        <div class="d-flex justify-content-center fs-5 fw-bold">
          Check the boxes of the fields you wish to display!
        </div>
        <div v-if="section.description" class="mx-3 d-flex justify-content-end">
          <i
            class="py-1 bi bi-info-circle"
            data-bs-toggle="tooltip"
            :data-bs-title="section.description"
            :style="{ color: uiConfiguration?.primaryTextColor }"
          />
        </div>

        <draggable
          :modelValue="accessForm.sections[index].elements"
          @update:modelValue="(newValue) => (accessForm.sections[index].elements = newValue)"
        >
          <template #item="{ element: criteria }">
            <div class="mb-4 mx-3 cursor-move d-flex">
              <div class="form-check form-check-inline align-middle">
                <input
                  id="inlineCheckbox1"
                  v-model="activeElements[index].selectedElements"
                  :value="criteria"
                  :required="criteria.required"
                  class="form-check-input"
                  :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
                  type="checkbox"
                />
                <label class="form-check-label" for="inlineCheckbox1">{{ value }}</label>
              </div>

              <div
                class="w-100"
                :style="
                  isElementActive(activeElements[index].selectedElements, criteria.id)
                    ? ''
                    : 'opacity: 0.5;'
                "
              >
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

                <div v-if="criteria.type === 'TEXT'" disabled>
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
                        :class="
                          validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''
                        "
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
                        :class="
                          validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''
                        "
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
            </div>
          </template>
        </draggable>
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
            class="btn"
            @click="props.nextTab()"
            :style="{
              'background-color': uiConfiguration.buttonColor,
              'border-color': uiConfiguration.buttonColor,
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
import { ref, computed, onMounted } from 'vue'
import { Tooltip } from 'bootstrap'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'
import AddFormSectionModal from '@/components/modals/AddFormSectionModal.vue'
import { FormWizard, TabContent } from 'vue3-form-wizard'
import { useNegotiationFormStore } from '../store/negotiationForm'
import { useNotificationsStore } from '../store/notifications'
import { useUiConfiguration } from '@/store/uiConfiguration.js'

import 'vue3-form-wizard/dist/style.css'
import { accessFormData } from '../composables/customizeForm.js'
import draggable from 'vuedraggable'

const uiConfigurationStore = useUiConfiguration()
const negotiationFormStore = useNegotiationFormStore()
const notificationsStore = useNotificationsStore()

const forceReRenderFormWizard = ref(null)

const notificationTitle = ref('')
const notificationText = ref('')
const negotiationCriteria = ref({})
const negotiationValueSets = ref({})
const validationColorHighlight = ref([])

const accessForm = ref(undefined)

const accessFormElements = ref([])
const openModal = ref(null)
const openAddFormSectionModal = ref(null)

let activeElements = ref([])

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

onMounted(async () => {
  new Tooltip(document.body, {
    selector: "[data-bs-toggle='tooltip']",
  })

  accessForm.value = accessFormData
  accessFormElements.value = await negotiationFormStore.retrieveFormElements()
  forceReRenderFormWizard.value += 1

  accessFormData.sections.forEach((section) => {
    activeElements.value.push({ id: section.id, selectedElements: [] })
    section.elements = accessFormElements.value
  })

  if (accessForm.value !== undefined) {
    initNegotiationCriteria()
  }
})

function addFormSection(name, label, description) {
  let newSection = JSON.parse(JSON.stringify(accessForm.value.sections[0]))

  newSection.id = accessForm.value.sections.length + 1
  newSection.name = name
  newSection.label = label
  newSection.description = description

  accessForm.value.sections.push(newSection)
  activeElements.value.push({ id: accessForm.value.sections.length, selectedElements: [] })

  initNegotiationCriteria()
  forceReRenderFormWizard.value += 1
}

async function getValueSet(id) {
  await negotiationFormStore.retrieveDynamicAccessFormsValueSetByID(id).then((res) => {
    negotiationValueSets.value[id] = res
  })
}

async function startNegotiation() {
  const postAccessForm = JSON.parse(JSON.stringify(accessForm.value))
  accessForm.value.sections.forEach((section, sectionIndex) => {
    postAccessForm.sections[sectionIndex].elements =
      activeElements.value[sectionIndex].selectedElements
  })
  await negotiationFormStore.addAccessForm(postAccessForm)
}

function startModal() {
  showNotification(
    'Confirm submission',
    "You will be redirected to the network page. Click 'Confirm' to proceed.",
  )
}

function showNotification(header, body) {
  openModal.value.click()
  notificationTitle.value = header
  notificationText.value = body
}

function handleFileUpload(event, section, criteria) {
  negotiationCriteria.value[section][criteria] = event.target.files[0]
}

function initNegotiationCriteria() {
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

function isElementActive(selectedElements, elementId) {
  return selectedElements.find((x) => x.id === elementId)
}
</script>

<style scoped>
.cursor-move,
label,
input,
textarea {
  cursor: move;
}
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
