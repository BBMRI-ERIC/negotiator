<template>
  <div
    v-if="isOpen"
    class="modal fade show"
    tabindex="-1"
    role="dialog"
    aria-modal="true"
    aria-labelledby="add-collaborator-title"
    style="display: block; background-color: rgba(0, 0, 0, 0.5)"
    @click="handleBackdropClick"
  >
    <div class="modal-dialog modal-dialog-centered" @click.stop>
      <div class="modal-content">
        <div class="modal-header">
          <h5 id="add-collaborator-title" class="modal-title">Add Collaborator</h5>
          <button
            type="button"
            class="btn-close"
            @click="close"
            aria-label="Close"
            :disabled="isLoading"
          ></button>
        </div>
        <div class="modal-body">
          <form id="add-collaborator-form" @submit.prevent="confirm">
            <output v-if="isSuccess" class="alert alert-success d-flex align-items-center">
              <i class="bi bi-check-circle-fill me-2"></i>
              <span>{{ collaboratorName }} has been added as a collaborator!</span>
            </output>
            <div v-else>
              <p class="mb-3">
                Enter the Subject ID of the user you want to add as a collaborator. Collaborators
                have the same access as the negotiation creator.
              </p>
              <p class="text-muted mb-3">
                <i class="bi bi-info-circle me-1"></i>
                The Subject ID can be found on the user's profile page.
              </p>
              <div class="mb-3">
                <label for="collaboratorSubjectIdInput" class="form-label fw-bold"
                  >Subject ID</label
                >
                <input
                  v-model="localSubjectId"
                  type="text"
                  class="form-control"
                  id="collaboratorSubjectIdInput"
                  placeholder="Enter Subject ID"
                  :disabled="isLoading"
                  @input="clearError"
                />
                <output
                  v-if="errorMessage"
                  for="collaboratorSubjectIdInput"
                  class="text-danger mt-2 d-block"
                >
                  {{ errorMessage }}
                </output>
              </div>
            </div>
          </form>
        </div>
        <div v-if="!isSuccess" class="modal-footer">
          <button
            type="button"
            class="btn btn-outline-secondary"
            @click="close"
            :disabled="isLoading"
          >
            Cancel
          </button>
          <button
            type="button"
            class="btn btn-primary"
            @click="confirm"
            :disabled="!localSubjectId.trim() || isLoading"
          >
            <span
              v-if="isLoading"
              class="spinner-border spinner-border-sm me-2"
              role="status"
            ></span>
            Add Collaborator
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '@/config/apiPaths'

const props = defineProps({
  isOpen: {
    type: Boolean,
    required: true,
  },
  negotiationId: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['update:isOpen', 'cancel', 'collaborator-added'])

const localSubjectId = ref('')
const errorMessage = ref('')
const isLoading = ref(false)
const isSuccess = ref(false)
const collaboratorName = ref('')

watch(
  () => props.isOpen,
  (newValue) => {
    if (!newValue) resetState()
  },
)

function handleBackdropClick() {
  if (!isLoading.value && !isSuccess.value) {
    close()
  }
}

function close() {
  emit('update:isOpen', false)
  emit('cancel')
  resetState()
}

function resetState() {
  localSubjectId.value = ''
  errorMessage.value = ''
  isLoading.value = false
  isSuccess.value = false
  collaboratorName.value = ''
}

function clearError() {
  errorMessage.value = ''
}

async function confirm() {
  if (!localSubjectId.value.trim()) {
    errorMessage.value = 'Subject ID is required.'
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    await axios.post(
      `${apiPaths.NEGOTIATION_PATH}/${props.negotiationId}/collaborators?subjectId=${encodeURIComponent(localSubjectId.value.trim())}`,
      {},
      { headers: getBearerHeaders() },
    )

    collaboratorName.value = 'The user'
    isSuccess.value = true
    emit('collaborator-added', { subjectId: localSubjectId.value.trim() })
    setTimeout(() => {
      close()
    }, 1500)
  } catch (error) {
    isLoading.value = false
    switch (error.response?.status) {
      case 400:
        errorMessage.value = 'Invalid Subject ID. Please check and try again.'
        break
      case 403:
        errorMessage.value = 'You are not authorized to add collaborators to this negotiation.'
        break
      case 404:
        errorMessage.value = 'Negotiation or user not found.'
        break
      case 409:
        errorMessage.value = 'This user is already a collaborator.'
        break
      default:
        errorMessage.value = 'An error occurred while adding the collaborator.'
    }
  }
}
</script>

<style scoped>
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1050;
}

.modal-content {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-header {
  border-bottom: 1px solid #e9ecef;
  padding: 1.25rem;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #212529;
}

.modal-body {
  padding: 1.5rem;
  font-size: 1rem;
  line-height: 1.6;
}

.modal-footer {
  border-top: 1px solid #e9ecef;
  padding: 1rem;
}

.text-danger {
  font-size: 0.9rem;
}

.btn-outline-secondary {
  border-radius: 6px;
  padding: 0.5rem 1rem;
  font-weight: 500;
}

.btn-primary {
  border-radius: 6px;
  padding: 0.5rem 1rem;
  font-weight: 500;
}

.btn-primary:disabled {
  background-color: #6c757d;
  border-color: #6c757d;
}
</style>
