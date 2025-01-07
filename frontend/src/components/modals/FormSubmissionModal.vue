<template>
  <div
      :id="id"
      class="modal"
      :class="{ fade: fade }"
      tabindex="-1"
      :aria-labelledby="`${id}Label`"
      aria-hidden="true"
  >
    <div
        class="modal-dialog modal-dialog-centered"
        :class="isModalSmall === true ? 'modal-sm' : 'modal-xl'"
    >
      <div class="modal-content">
        <div class="modal-header justify-content-center">
          <h4 class="modal-title text-center">
            {{ title }}
          </h4>
          <button
              v-if="isXButtondisplayed"
              type="button"
              class="btn-close"
              data-bs-dismiss="modal"
              aria-label="Close"
          />
        </div>
        <div class="modal-body text-left">
          <form-wizard
              v-if="accessForm"
              :start-index="0"
              color="var(--bs-secondary)"
              step-size="md"
              @on-complete="startModal"
          >
            <tab-content
                v-for="section in accessForm.sections"
                :key="section.name"
                :title="section.label"
                class="form-step border rounded-2 px-2 py-3 mb-2 overflow-auto text-primary-text"
                :before-change="isSectionValid(section)"
            >
              <div
                  v-if="section.description"
                  class="mx-3 d-flex justify-content-end"
              >
                <i
                    class="py-1 bi bi-info-circle"
                    data-bs-toggle="tooltip"
                    :data-bs-title="section.description"
                />
              </div>

              <div
                  v-for="criteria in section.elements"
                  :key="criteria.name"
                  class="mb-4 mx-3"
              >
                <label
                    class="form-label text-primary-text"
                    :class="{ required: criteria.required }"
                >
                  {{ criteria.label }}
                </label>

                <span
                    v-if="criteria.description"
                    class="ms-2 text-muted"
                >
                  <i
                      class="py-1 bi bi-info-circle"
                      data-bs-toggle="tooltip"
                      :data-bs-title="criteria.description"
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
                  >
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
                    >
                    <label
                        class="form-check-label"
                        for="inlineRadio1"
                    > Yes </label>
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
                    >
                    <label
                        class="form-check-label"
                        for="inlineRadio2"
                    > No </label>
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
                      >
                      <label
                          class="form-check-label"
                          for="inlineCheckbox1"
                      >{{ value }}</label>
                    </div>
                  </div>
                  <div
                      v-if="negotiationValueSets[criteria.id]?.externalDocumentation && negotiationValueSets[criteria.id]?.externalDocumentation !== 'none'"
                  >
                    <span class="text-muted"> External Documentation - </span>
                    <a :href="negotiationValueSets[criteria.id]?.externalDocumentation"> {{
                        negotiationValueSets[criteria.id]?.externalDocumentation
                      }} </a>
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
                      >
                      <label
                          class="form-check-label"
                          for="inlineRadio1"
                      >{{ value }}</label>
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

                <div
                    v-else-if="criteria.type === 'NUMBER'"
                    class="col-5"
                >
                  <input
                      v-model="negotiationCriteria[section.name][criteria.name]"
                      :type="criteria.type"
                      :placeholder="criteria.description"
                      class="form-control text-secondary-text"
                      :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
                      :required="criteria.required"
                      @keypress="isNumber($event)"
                  >
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
                  >
                </div>

                <div
                    v-else-if="criteria.type === 'DATE'"
                    class="w-25"
                >
                  <p
                      v-if="criteria.description"
                      class="text-muted"
                  >
                    {{ criteria.description }}
                  </p>
                  <input
                      id="startDate"
                      v-model="negotiationCriteria[section.name][criteria.name]"
                      value=""
                      class="form-control form-control-sm"
                      :class="validationColorHighlight.includes(criteria.name) ? 'is-invalid' : ''"
                      type="date"
                  >
                </div>

                <div v-else-if="criteria.type === 'INFORMATION'">
                  <p
                      v-if="criteria.description"
                      class="text-muted"
                  >
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
                >

                <div
                    v-if="validationColorHighlight.includes(criteria.name)"
                    class="invalidText"
                >
                  {{ transformMessage(criteria.type) }}
                </div>
                <div
                    v-if="negotiationValueSets[criteria.id]?.externalDocumentation && negotiationValueSets[criteria.id]?.externalDocumentation !== 'none'"
                    class="mt-2"
                >
                  <span class="text-muted"> External Documentation - </span>
                  <a :href="negotiationValueSets[criteria.id]?.externalDocumentation"> {{
                      negotiationValueSets[criteria.id]?.externalDocumentation
                    }} </a>
                  {{ props.requirementId }}
                </div>
              </div>
            </tab-content>
            <tab-content
                title="Overview"
                class="form-step overflow-auto"
            >
              <div
                  v-for="section in accessForm.sections"
                  :key="section.name"
                  class="border rounded-2 input-group p-3 mb-2 mb-3 text-secondary-text"
              >
                <span class="mb-3 fs-4 fw-bold text-secondary">{{ section.label.toUpperCase() }}</span>
                <div
                    v-for="accessFormElement in section.elements"
                    :key="accessFormElement.name"
                    class="input-group mb-2"
                >
                  <label class="me-2 fw-bold">{{ accessFormElement.label }}:</label>
                  <span v-if="isAttachment(negotiationCriteria[section.name][accessFormElement.name])">
                    <span v-if="negotiationCriteria[section.name][accessFormElement.name].name">{{
                        negotiationCriteria[section.name][accessFormElement.name].name
                      }}</span>
                    <div
                        v-for="(choice, index) in negotiationCriteria[section.name][accessFormElement.name]"
                        v-else
                        :key="index"
                    >{{ choice }}</div>
                  </span>
                  <span v-else>
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
                    class="btn btn-secondary"
                    @click="props.prevTab()"
                >
                  Previous
                </button>
              </div>
              <div class="wizard-footer-right">
                <button
                    class="btn btn-secondary"
                    @click="props.nextTab()"
                >
                  {{ props.isLastStep ? "Submit request" : "Next" }}
                </button>
              </div>
            </template>
          </form-wizard>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from "vue"
import {Tooltip} from "bootstrap"
import {FormWizard, TabContent} from "vue3-form-wizard"
import "vue3-form-wizard/dist/style.css"
import {useRouter} from "vue-router"
import {useFormsStore} from "../../store/forms"
import {useNotificationsStore} from "../../store/notifications"

const props = defineProps({
  id: {
    type: String,
    required: true
  },
  negotiationId: {
    type: String,
    required: true
  },
  requirementId: {
    type: String,
    required: true
  },
  resourceId: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  fade: {
    type: Boolean,
    required: false,
    default: true
  },
  isModalSmall: {
    type: Boolean,
    required: false,
    default: false
  },
  isXButtondisplayed: {
    type: Boolean,
    required: false,
    default: true
  },
  requiredAccessFormId: {
    type: String,
    required: true,
    default: undefined
  },
  requirementLink: {
    type: String,
    required: true,
    default: undefined
  }
})

const formsStore = useFormsStore()
const notificationsStore = useNotificationsStore()

const router = useRouter()

const notificationTitle = ref("")
const notificationText = ref("")
const negotiationCriteria = ref({})
const negotiationValueSets = ref({})
const validationColorHighlight = ref([])

const accessForm = ref(undefined)
const resources = ref([])
const humanReadableSearchParameters = ref([])
const openModal = ref(null)

const loading = computed(() => {
  return accessForm.value === undefined
})

const queryParameters = computed(() => {
  return humanReadableSearchParameters.value.split("\r\n")
})

watch(() => props.requiredAccessFormId, async (first, second) => {
  if (props.requiredAccessFormId !== undefined) {
    accessForm.value = await loadAccessForm(props.requiredAccessFormId)
    if (accessForm.value !== undefined) {
      initNegotiationCriteria()
    }
  }
})
onMounted(() => {
  new Tooltip(document.body, {
    selector: "[data-bs-toggle='tooltip']"
  })
})

async function loadAccessForm(id) {
  return formsStore.retrieveAccessFormById(id)
}

function backToNegotiation(id) {
  router.push("/negotiations/" + id + "/ROLE_RESEARCHER")
}

async function getValueSet(link, id) {
  await formsStore.retrieveDynamicAccessFormsValueSetByLink(link).then((res) => {
    negotiationValueSets.value[id] = res
  })
}

async function startRequiredAccessForm() {
  const data = {
    resourceId: props.resourceId,
    payload: negotiationCriteria.value
  }
  const negotiationId = props.negotiationId
  const requirementId = props.requirementId
  await formsStore.submitRequiredInformation(data, negotiationId, requirementId)
}

function startModal() {
  startRequiredAccessForm()
  emitConfirm()
}

function isAttachment(value) {
  return value instanceof File || value instanceof Object
}

function handleFileUpload(event, section, criteria) {
  negotiationCriteria.value[section][criteria] = event.target.files[0]
}

function showNotification(header, body) {
  notificationTitle.value = header
  notificationText.value = body
}

function initNegotiationCriteria() {
  for (const section of accessForm.value.sections) {
    negotiationCriteria.value[section.name] = {}
    for (const criteria of section.elements) {
      if (criteria.type === "MULTIPLE_CHOICE") {
        negotiationCriteria.value[section.name][criteria.name] = []
        getValueSet(criteria._links["value-set"].href)
      } else if (criteria.type === "SINGLE_CHOICE") {
        getValueSet(criteria._links["value-set"].href, criteria.id)
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
    section.elements.forEach(ac => {
      if (ac.required) {
        if (ac.type === "MULTIPLE_CHOICE" && Object.keys(negotiationCriteria.value[section.name][ac.name]).length === 0) {
          validationColorHighlight.value.push(ac.name)
          valid = false
        } else if (ac.type !== "MULTIPLE_CHOICE" && (typeof negotiationCriteria.value[section.name][ac.name] !== "string" || negotiationCriteria.value[section.name][ac.name] === "")) {
          validationColorHighlight.value.push(ac.name)
          valid = false
        }
      } else if (valid) {
        valid = true
      }
    })
    if (!valid) {
      notificationsStore.setNotification("Please fill out all required fields correctly")
    }
    return valid
  }
}

function uncheckRadioButton(value, sectionName, criteriaName) {
  if (negotiationCriteria.value[sectionName][criteriaName] === value) {
    negotiationCriteria.value[sectionName][criteriaName] = ""
  }
}

function translateTrueFalse(value) {
  if (typeof value === "boolean") {
    return value ? "Yes" : "No"
  }
  return value
}

function isNumber(evt) {
  const charCode = evt.which ? evt.which : evt.keyCode
  if (
      charCode > 31 &&
      (charCode < 48 || charCode > 57) &&
      charCode !== 46
  ) {
    evt.preventDefault()
  }
}

function transformMessage(text) {
  if (text == "SINGLE_CHOICE" || text == "BOOLEAN") {
    return "Please select one of available values"
  } else if (text == "MULTIPLE_CHOICE") {
    return "Please select at least one of the available values"
  } else if (text == "TEXT_LARGE") {
    return "Please provide a text"
  } else {
    return "Please provide a " + text?.toLowerCase()
  }
}

const emit = defineEmits(["confirm"])

function emitConfirm() {
  showNotification(
      "Confirm submission",
      "Thank you. Your response has been submitted successfully"
  )
  emit("confirm")
}
</script>

<style scoped>
.required:after {
  content: "  *\00a0";
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
