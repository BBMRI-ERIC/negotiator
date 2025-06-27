<!-- InformationRequirementModal.vue -->
<template>
  <div>
    <!-- Modal Backdrop -->
    <div v-if="show" class="modal-backdrop fade show"></div>

    <!-- Modal -->
    <div
      v-if="show"
      class="modal fade show"
      tabindex="-1"
      aria-labelledby="requirementModalLabel"
      aria-hidden="true"
      style="display: block"
    >
      <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content">
          <div class="modal-header justify-content-center">
            <h4 class="modal-title text-center">Create New Requirement</h4>
          </div>
          <div class="modal-body">
            <form @submit.prevent="setInfoRequirements">
              <div class="mb-3">
                <label for="linkedAccessForm" class="form-label">Linked Access Form</label>
                <select
                  id="linkedAccessForm"
                  v-model="selectedAccessForm"
                  class="form-control"
                  required
                >
                  <option :value="null" disabled>Select a form...</option>
                  <option v-for="(form, index) in accessForms" :key="index" :value="form">
                    {{ form.name }}
                  </option>
                </select>
                <small class="form-text text-muted">
                  Select the access form to link with this requirement.
                </small>
              </div>
              <div class="mb-3">
                <label for="linkedLifecycleEvent" class="form-label">Linked Lifecycle Event</label>
                <select
                  id="linkedLifecycleEvent"
                  v-model="selectedEvent"
                  class="form-control"
                  required
                >
                  <option :value="null" disabled>Select an event...</option>
                  <option v-for="(event, index) in resourceAllEvents" :key="index" :value="event">
                    {{ event.label }}
                  </option>
                </select>
                <small class="form-text text-muted">
                  Select the lifecycle event associated with this requirement.
                </small>
              </div>
              <div class="mb-3">
                <label class="form-label">Summary Only for Admin</label>
                <div>
                  <div class="form-check form-check-inline">
                    <input
                      class="form-check-input"
                      type="radio"
                      name="summaryOnlyForAdmin"
                      id="adminYes"
                      :value="true"
                      v-model="summaryOnlyForAdmin"
                    />
                    <label class="form-check-label" for="adminYes">Yes</label>
                  </div>
                  <div class="form-check form-check-inline">
                    <input
                      class="form-check-input"
                      type="radio"
                      name="summaryOnlyForAdmin"
                      id="adminNo"
                      :value="false"
                      v-model="summaryOnlyForAdmin"
                    />
                    <label class="form-check-label" for="adminNo">No</label>
                  </div>
                </div>
                <small class="form-text text-muted">
                  Choose whether the summary is viewable only by admins.
                </small>
              </div>
            </form>
          </div>
          <div class="modal-footer justify-content-center">
            <button
              type="button"
              class="btn btn-primary me-2"
              @click="setInfoRequirements"
              :disabled="!isFormValid"
            >
              Create
            </button>
            <button type="button" class="btn btn-close-custom" @click="closeModal">Close</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

defineProps({
  show: {
    type: Boolean,
    required: true,
  },
  accessForms: {
    type: Array,
    required: true,
  },
  resourceAllEvents: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['set-info-requirements', 'close-modal'])

const selectedAccessForm = ref(null)
const selectedEvent = ref(null)
const summaryOnlyForAdmin = ref(true)

const isFormValid = computed(() => {
  return selectedAccessForm.value?.id && selectedEvent.value?.value
})

function setInfoRequirements() {
  if (!isFormValid.value) return
  const data = {
    requiredAccessFormId: selectedAccessForm.value.id,
    forResourceEvent: selectedEvent.value.value,
    viewableOnlyByAdmin: summaryOnlyForAdmin.value,
  }
  emit('set-info-requirements', data)
  closeModal()
  resetForm()
}

function closeModal() {
  emit('close-modal')
}

function resetForm() {
  selectedAccessForm.value = null
  selectedEvent.value = null
  summaryOnlyForAdmin.value = true
}
</script>

<style scoped>
.modal-body {
  max-height: 70vh;
  overflow-y: auto;
}

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1040;
}

.modal-header,
.modal-footer {
  justify-content: center;
}

.btn-close-custom {
  background: gray;
  border-color: gray;
  outline-color: gray;
  color: white;
}

.btn-close-custom:hover {
  background: darkgray;
}
</style>
