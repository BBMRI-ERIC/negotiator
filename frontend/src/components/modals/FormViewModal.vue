<template>
  <div
    class="modal"
    :class="{ fade: fade }"
    tabindex="-1"
    :aria-labelledby="`${id}Label`"
    aria-hidden="true"
  >
    <div
      class="modal-dialog modal-dialog-centered"
      :class="isModalSmall === true ? 'modal-m' : 'modal-xl'"
    >
      <div class="modal-content">
        <div class="modal-header">
          <button
            v-if="isXButtondisplayed"
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          />
        </div>
        <div class="modal-body text-left mb-3 mx-3">
          <div class="fs-3 mb-4 fw-bold text-secondary text-center">Submitted Information</div>
          <div class="justify-content-end align-items-center mb-2 d-flex d-row">
            <button
              v-if="!submittedForm?.submitted && !isAdmin"
              type="button"
              class="btn btn-sm edit-button"
              @click="$emit('editInfoSubmission')"
            >
              <i class="bi bi-pencil-square" />
              Edit
            </button>
            <div v-if="isAdmin" class="form-check form-switch">
                <input
                  :value="submittedForm?.submitted"
                  class="form-check-input"
                  type="checkbox"
                  role="switch"
                  @change="changeEditing()"
                />
                <label class="form-check-label" for="flexSwitchCheckDefault"> allow editing </label>
            </div>
          </div>
          <div>
            <ul class="ps-0">
              <li
                v-for="(element, key) in props.submittedForm?.payload"
                :key="element"
                class="list-group-item p-3"
              >
                <span class="fs-5 fw-bold text-primary-text mt-3"> {{ key }}</span>
                <div v-for="(subelement, subelementkey) in element" :key="subelement" class="mt-3">
                  <label class="me-2 fw-bold text-secondary-text">{{ subelementkey }}:</label>
                  <span v-if="isAttachment(subelement)">
                    <span v-if="subelement.name" class="d-flex col">
                      <span class="text-truncate" :title="subelement.name">{{
                        subelement.name
                      }}</span>
                      <font-awesome-icon
                        v-if="isAttachment(subelement)"
                        class="ms-1 cursor-pointer"
                        icon="fa fa-download"
                        fixed-width
                        @click.prevent="downloadAttachment(subelement.id, subelement.name)"
                      />
                    </span>
                    <span v-else>
                      <div v-for="(choice, index) in subelement" :key="index">
                        {{ choice }}
                      </div>
                    </span>
                  </span>
                  <span v-else class="text-break">
                    {{ translateTrueFalse(subelement) }}
                  </span>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { Tooltip } from 'bootstrap'
import 'vue3-form-wizard/dist/style.css'
import { useNegotiationPageStore } from '../../store/negotiationPage.js'
import { useFormsStore } from '../../store/forms'

const negotiationPageStore = useNegotiationPageStore()
const formsStore = useFormsStore()

const props = defineProps({
  submittedForm: {
    type: Object,
    required: true,
  },
  isModalSmall: {
    type: Boolean,
    required: false,
    default: true,
  },
  isXButtondisplayed: {
    type: Boolean,
    required: false,
    default: true,
  },
  isSubmittedFormSubmitted: {
    type: Boolean,
    required: false,
    default: false,
  },
  isAdmin: { 
    type: Boolean,
    default: false 
  },
})

function translateTrueFalse(value) {
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }
  return value
}

onMounted(() => {
  new Tooltip(document.body, {
    selector: "[data-bs-toggle='tooltip']",
  })
})

function isAttachment(value) {
  return value instanceof Object
}

function downloadAttachment(id, name) {
  negotiationPageStore.downloadAttachment(id, name)
}

function changeEditing() {
  const data = {
      submitted: true,
  }

 formsStore.updateInfoSubmissionsisedit(props.submittedForm.id, data)
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
.edit-button:hover {
  background-color: lightgray;
  color: #212529;
}
</style>
