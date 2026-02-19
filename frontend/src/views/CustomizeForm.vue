<template>
  <div>
    <button ref="addModal" hidden data-bs-toggle="modal" data-bs-target="#feedbackAddModal" />
    <confirmation-modal
      id="feedbackAddModal"
      :title="'Confirm submission'"
      :text="'You will be redirected to the Home page. Click Confirm to proceed.'"
      :message-enabled="false"
      dismiss-button-text="Back to HomePage"
      @confirm="addAccessForm"
    />
    <button
      ref="duplicateModal"
      hidden
      data-bs-toggle="modal"
      data-bs-target="#feedbackDuplicateModal"
    />
    <confirmation-modal
      id="feedbackDuplicateModal"
      :title="'Confirm Duplication'"
      :text="'You will be redirected to the Home page. Click Confirm to proceed.'"
      :message-enabled="false"
      dismiss-button-text="Back to HomePage"
      @confirm="addAccessForm"
    />
    <button ref="editModal" hidden data-bs-toggle="modal" data-bs-target="#feedbackEditModal" />
    <confirmation-modal
      id="feedbackEditModal"
      :title="'Confirm Editing'"
      :text="'You will be redirected to the Home page. Click Confirm to proceed.'"
      :message-enabled="false"
      dismiss-button-text="Back to HomePage"
      @confirm="editAccessForm"
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
      @confirm="addFormSection"
    />
    <div class="new-section-button">
      <h1>{{ `${typeAccessForm + ' Access Form'}` }}</h1>
      <label :style="{ color: uiConfiguration?.primaryTextColor }">
        This form allows for the inclusion of multiple sections to accommodate various data inputs.
        If your situation requires additional details, or if you are submitting information related
        to multiple items or categories, please click the button below to add a new section. Each
        added section will function independently, allowing for distinct data entry. You can add as
        many sections as needed to accurately reflect your information.
      </label>
      <button
        @click="openAddFormSectionModal.click()"
        type="button"
        class="btn btn-sm sm btn-danger my-3 py-1 px-1 me-md-5"
      >
        + Add new section
      </button>
    </div>
    <div class="form-name" v-if="accessForm">
      <label class="form-label" :style="{ color: uiConfiguration?.primaryTextColor }">
        Form Name
      </label>
      <input
        v-model="accessForm.name"
        type="TEXT"
        placeholder="Give a form name"
        class="form-control text-secondary-text w-25"
      />
    </div>

    <form-wizard
      :key="forceReRenderFormWizard"
      v-if="accessForm && accessForm.sections.length"
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
      >
        <div class="d-flex me-3 justify-content-end">
          <button type="button" class="btn btn-danger btn-sm" v-on:click="removeSection(index)">
            remove section
          </button>
        </div>
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
          @update:modelValue="
            (newValue) => (
              (accessForm.sections[index].elements = newValue),
              updateSectionsWithNewOrderElements(section.name)
            )
          "
        >
          <template #item="{ element: criteria }">
            <div class="mb-4 mx-3 d-flex">
              <div class="form-check form-check-inline align-middle">
                <input
                  id="inlineCheckbox1"
                  @change="changeActiveElements(index, criteria, $event)"
                  :value="criteria"
                  :required="criteria.required"
                  :checked="isElementActive(activeElements[index].selectedElements, criteria.id)"
                  class="form-check-input"
                  :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
                  type="checkbox"
                />
                <label class="form-check-label" for="inlineCheckbox1">{{ criteria.labe }}</label>
              </div>

              <div
                class="w-100 section-elements"
                :style="
                  isElementActive(activeElements[index].selectedElements, criteria.id)
                    ? ''
                    : 'opacity: 0.5;'
                "
              >
                <div class="d-flex space-beatwine justify-content-between">
                  <div>
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
                  </div>
                  <div class="form-check">
                    <input
                      :id="`inlineCheckboxRequired-${criteria.id}`"
                      @change="
                        (changeRequriedElements(index, criteria),
                        (criteria.required = !criteria.required))
                      "
                      :value="criteria.required"
                      :checked="criteria.required"
                      class="form-check-input form-check-input-requiried"
                      :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
                      type="checkbox"
                    />
                    <label class="form-check-label" :for="`inlineCheckboxRequired-${criteria.id}`"
                      >Required field</label
                    >
                  </div>
                </div>

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
                        :class="
                          validationColorHighlight && validationColorHighlight && validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''
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
                          validationColorHighlight && validationColorHighlight && validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''
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
            {{ props.isLastStep ? `${typeAccessForm + ' Access Form'}` : 'Next' }}
          </button>
        </div>
      </template>
    </form-wizard>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Tooltip } from 'bootstrap'
import { useUserStore } from '../store/user.js'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'
import AddFormSectionModal from '@/components/modals/AddFormSectionModal.vue'
import { FormWizard, TabContent } from 'vue3-form-wizard'
import { useNegotiationFormStore } from '../store/negotiationForm'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import 'vue3-form-wizard/dist/style.css'
import draggable from 'vuedraggable'
import { useRouter, useRoute } from 'vue-router'

const props = defineProps({
  typeAccessForm: {
    type: String,
    default: '',
  },
})

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const uiConfigurationStore = useUiConfiguration()
const negotiationFormStore = useNegotiationFormStore()
const forceReRenderFormWizard = ref(null)
const negotiationCriteria = ref({})
const negotiationValueSets = ref({})
const validationColorHighlight = ref([])
const accessForm = ref({
  id: 1,
  name: 'BBMRI Template',
  sections: [],
})
const nonEditedAccessForm = ref({})
const accessFormElements = ref([])
var requriedElements = ref([])
const addModal = ref(null)
const duplicateModal = ref(null)
const editModal = ref(null)

const openAddFormSectionModal = ref(null)
let activeElements = ref([])
const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
  new Tooltip(document.body, {
    selector: "[data-bs-toggle='tooltip']",
  })
  accessFormElements.value = await negotiationFormStore.retrieveFormElements()
  // edit-duplicate section
  if (route.params.accessFormId) {
    await negotiationFormStore
      .retrieveAccessFormById(route.params.accessFormId)
      .then((response) => {
        nonEditedAccessForm.value = response
        accessForm.value.id = response.id
        accessForm.value.name = response.name
      })

    nonEditedAccessForm.value.sections.forEach((section) => {
      addFormSection(section.name, section.label, section.description, section.elements)
      activeElements.value[activeElements.value.length - 1].selectedElements = [...section.elements]
    })

    // Put elements that are selected by user first in each section
    accessForm.value.sections.forEach((section, sectionIndex) => {
      const selectedElements = activeElements.value[sectionIndex].selectedElements

      const selectedIds = selectedElements.map((item) => item.id)
      const reorderedElements = [
        ...selectedElements,
        ...section.elements.filter((item) => !selectedIds.includes(item.id)),
      ]
      section.elements = reorderedElements
    })

    forceReRenderFormWizard.value += 1
  }

  forceReRenderFormWizard.value += 1
  if (accessForm.value !== undefined) {
    initNegotiationCriteria()
  }
})
function addFormSection(name, label, description, elements) {
  let newSection = {
    id: 1,
    name: 'project',
    label: 'Project',
    description: 'Provide information about your project',
    elements: [],
  }
  newSection.id = accessForm.value.sections.length + 1
  newSection.name = name
  newSection.label = label
  newSection.description = description
  newSection.elements = accessFormElements.value
  accessForm.value.sections.push(newSection)
  activeElements.value.push({
    id: accessForm.value.sections.length,
    selectedElements: elements || [],
  })
  // add sections as array to requriedElements
  requriedElements.value.push([])

  initNegotiationCriteria()
  forceReRenderFormWizard.value += 1
}
async function getValueSet(id) {
  await negotiationFormStore.retrieveDynamicAccessFormsValueSetByID(id).then((res) => {
    negotiationValueSets.value[id] = res
  })
}
async function addAccessForm() {
  const postAccessForm = JSON.parse(JSON.stringify(accessForm.value))
  let accessFormId = undefined
  await negotiationFormStore.addAccessForm(postAccessForm).then((resp) => {
    accessFormId = resp.id
  })
  accessForm.value.sections.forEach((section, sectionIndex) => {
    sortActiveElements(
      accessForm.value.sections[sectionIndex].elements,
      activeElements.value[sectionIndex].selectedElements,
      sectionIndex,
    )

    postAccessForm.sections[sectionIndex].elements =
      activeElements.value[sectionIndex].selectedElements
    const sections = {
      name: section.name,
      label: section.label,
      description: section.description,
    }

    negotiationFormStore.addAccessFormSections(sections).then((section) => {
      const currentSection = {
        sectionId: section.id,
        sectionOrder: sectionIndex,
      }
      negotiationFormStore.linkSectionToAccessForm(accessFormId, currentSection).then(() => {
        postAccessForm.sections[sectionIndex].elements.forEach((element, elementIndex) => {
          let currentElement = {
            elementId: element.id,
            elementOrder: elementIndex,
            required: false,
          }
          try {
            negotiationFormStore.linkElementsToSectionToAccessForm(
              accessFormId,
              section.id,
              currentElement,
            )
          } catch (e) {
            console.error('error linking elements to section', e)
          }
        })
      })
    })
  })

  goToAdminSettingsPage()
}

function compareSections(json1, json2) {
  const sections1 = json1.sections || []
  const sections2 = json2.sections || []

  const getSectionIds = (sections) => sections.map((s) => s.name)

  const ids1 = new Set(getSectionIds(sections1))
  const ids2 = new Set(getSectionIds(sections2))

  const added = sections2.filter((s) => !ids1.has(s.name))
  const removed = sections1.filter((s) => !ids2.has(s.name))

  return {
    added,
    removed,
  }
}

async function editAccessForm() {
  const postAccessForm = JSON.parse(JSON.stringify(accessForm.value))
  let accessFormId = accessForm.value.id

  // unlink removed sections from accessForm
  compareSections(nonEditedAccessForm.value, accessForm.value).removed.forEach((section) => {
    try {
      negotiationFormStore.unlinkSectionFromAccessForm(accessFormId, section.id)
    } catch (e) {
      console.error('Error unlinking access form from sections', e)
    }
  })

  accessForm.value.sections.forEach((section, sectionIndex) => {
    sortActiveElements(
      accessForm.value.sections[sectionIndex].elements,
      activeElements.value[sectionIndex].selectedElements,
      sectionIndex,
    )

    postAccessForm.sections[sectionIndex].elements =
      activeElements.value[sectionIndex].selectedElements

    const sections = {
      name: section.name,
      label: section.label,
      description: section.description,
    }

    // link new sections to accessForm
    if (
      compareSections(nonEditedAccessForm.value, accessForm.value).added.some(
        (e) => e.name === section.name,
      )
    ) {
      negotiationFormStore.addAccessFormSections(sections).then((section) => {
        const currentSection = {
          sectionId: section.id,
          sectionOrder: sectionIndex,
        }

        negotiationFormStore.linkSectionToAccessForm(accessFormId, currentSection).then(() => {
          postAccessForm.sections[sectionIndex].elements.forEach((element, elementIndex) => {
            let currentElement = {
              elementId: element.id,
              elementOrder: elementIndex,
              required: false,
            }
            try {
              negotiationFormStore.linkElementsToSectionToAccessForm(
                accessFormId,
                section.id,
                currentElement,
              )
            } catch (e) {
              console.error('error linking elements to section', e)
            }
          })
        })
      })
    } else {
      // unlink removed elements from section
      const originalSection = nonEditedAccessForm.value.sections.find(
        (s) => s.name === section.name,
      )
      if (originalSection) {
        const currentElementIds = new Set(
          activeElements.value[sectionIndex].selectedElements.map((el) => el.id),
        )

        originalSection.elements.forEach((element) => {
          const elementRequiredDiff =
            requriedElements.value[sectionIndex][element.id] !== undefined &&
            requriedElements.value[sectionIndex][element.id] !== element.required
          if (!currentElementIds.has(element.id) || elementRequiredDiff) {
            try {
              negotiationFormStore
                .deleteLinkElementFromSectionInAccessForm(accessFormId, section.id, element.id)
                .then(() => {
                  if (elementRequiredDiff) {
                    // link new elements to section
                    postAccessForm.sections[sectionIndex].elements.forEach(
                      (element, elementIndex) => {
                        let currentElement = {
                          elementId: element.id,
                          elementOrder: elementIndex + 1,
                          required: element.required,
                        }
                        try {
                          negotiationFormStore.linkElementsToSectionToAccessForm(
                            accessFormId,
                            section.id,
                            currentElement,
                          )
                        } catch (e) {
                          console.error('error linking elements to section', e)
                        }
                      },
                    )
                  }
                })
            } catch (e) {
              console.error('Error unlinking element from section in access form', e)
            }
          }
        })
      }

      // link new elements to section
      postAccessForm.sections[sectionIndex].elements.forEach((element, elementIndex) => {
        if (
          !originalSection ||
          !originalSection.elements.some((e) => e.id === element.id) ||
          sectionsWithNewOrderElements.value[section.name]
        ) {
          let currentElement = {
            elementId: element.id,
            elementOrder: elementIndex + 1,
            required: element.required,
          }
          try {
            negotiationFormStore.linkElementsToSectionToAccessForm(
              accessFormId,
              section.id,
              currentElement,
            )
          } catch (e) {
            console.error('error linking elements to section', e)
          }
        }
      })
    }
  })

  goToAdminSettingsPage()
}

function goToAdminSettingsPage() {
  router.push({
    name: 'settings',
  })
}

function startModal() {
  if (props.typeAccessForm === 'Edit') {
    editModal.value.click()
  } else if (props.typeAccessForm === 'Duplicate') {
    duplicateModal.value.click()
  } else {
    addModal.value.click()
  }
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
  return selectedElements.find((x) => x.id === elementId) ? true : false
}

function changeActiveElements(elementIndex, element, event) {
  if (event.target.checked) {
    // If the checkbox is checked, add the element to activeElements
    activeElements.value[elementIndex].selectedElements.push(element)
  } else {
    // If the checkbox is unchecked, remove the element from activeElements
    const index = activeElements.value[elementIndex].selectedElements.findIndex(
      (x) => x.id === element.id,
    )
    if (index !== -1) {
      activeElements.value[elementIndex].selectedElements.splice(index, 1)
    }
  }
}
function changeRequriedElements(index, criteria) {
  requriedElements.value[index][criteria.id] = criteria.required
}

function sortActiveElements(referenceArray, arrayToSort, sectionIndex) {
  // Sort the arrayToSort based on the order in the referenceArray
  arrayToSort.sort((a, b) => {
    return (
      referenceArray.findIndex((item) => item.id === a.id) -
      referenceArray.findIndex((item) => item.id === b.id)
    )
  })

  activeElements.value[sectionIndex].selectedElements = arrayToSort
}

function removeSection(sectionIndex) {
  accessForm.value.sections.splice(sectionIndex, 1)
  requriedElements.value.splice(sectionIndex, 1)
  forceReRenderFormWizard.value += 1
}

const sectionsWithNewOrderElements = ref({})

function updateSectionsWithNewOrderElements(sectionName) {
  if (sectionName) {
    sectionsWithNewOrderElements.value[sectionName] = sectionName
  }
}
</script>
<style scoped>
.section-elements {
  cursor: move;
}
.required:after {
  content: '  *\00a0';
  color: red;
}
.form-check-input-requiried:checked {
  background-color: var(--bs-danger);
  border-color: var(--bs-danger);
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
