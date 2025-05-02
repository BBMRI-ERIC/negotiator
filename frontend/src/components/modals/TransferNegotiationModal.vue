<template>
  <div
    v-if="isOpen"
    class="modal fade show"
    tabindex="-1"
    style="display: block; background-color: rgba(0, 0, 0, 0.5);"
    @click="handleBackdropClick"
  >
    <div class="modal-dialog modal-dialog-centered" @click.stop>
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">Transfer Negotiation</h5>
          <button
            type="button"
            class="btn-close"
            @click="close"
            aria-label="Close"
          ></button>
        </div>
        <div class="modal-body">
          <p class="mb-3">
            You are about to transfer this negotiation to another user. Enter the Subject ID of the
            user who will become the new owner. This action will reassign all responsibilities and
            permissions associated with the negotiation to the specified user.
          </p>
          <p class="text-muted mb-3">
            <i class="bi bi-info-circle me-1"></i>
            The Subject ID can be found on the user’s profile page.
          </p>
          <div class="alert alert-warning d-flex align-items-center mb-3" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            <div>
              <strong>Warning:</strong> If the transfer is successful, you will lose access to this
              negotiation.
            </div>
          </div>
          <div class="mb-3">
            <label for="subjectIdInput" class="form-label fw-bold">Subject ID</label>
            <input
              v-model="localSubjectId"
              type="text"
              class="form-control"
              id="subjectIdInput"
              placeholder="Enter Subject ID"
              @input="clearError"
            />
            <div v-if="errorMessage" class="text-danger mt-2">{{ errorMessage }}</div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline-secondary" @click="close">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            @click="confirm"
            :disabled="!localSubjectId.trim()"
          >
            Confirm
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  isOpen: {
    type: Boolean,
    required: true
  },
  subjectId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:isOpen', 'update:subjectId', 'confirm', 'cancel'])

const localSubjectId = ref(props.subjectId)
const errorMessage = ref('')

// Sync localSubjectId with props.subjectId
watch(() => props.subjectId, (newValue) => {
  localSubjectId.value = newValue
})

// Sync localSubjectId back to parent
watch(localSubjectId, (newValue) => {
  emit('update:subjectId', newValue)
})

function handleBackdropClick() {
  close()
}

function close() {
  emit('update:isOpen', false)
  emit('cancel')
}

function clearError() {
  errorMessage.value = ''
}

function confirm() {
  if (!localSubjectId.value.trim()) {
    errorMessage.value = 'Subject ID is required.'
    return
  }
  emit('confirm', localSubjectId.value.trim())
  close()
}
</script>

<style scoped>
/* Modal styles */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1050;
}

.modal.fade .modal-dialog {
  transition: transform 0.3s ease-out;
  transform: translate(0, -50px);
}

.modal.fade.show .modal-dialog {
  transform: translate(0, 0);
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

.modal-body p {
  margin-bottom: 1rem;
}

.modal-body .text-muted {
  font-size: 0.9rem;
}

.alert-warning {
  background-color: #fff3cd;
  border-color: #ffeeba;
  color: #856404;
  font-size: 0.95rem;
  padding: 0.75rem 1rem;
}

.alert-warning i {
  font-size: 1.2rem;
  color: #856404;
}

.form-label {
  font-size: 1rem;
  color: #212529;
}

.form-control {
  border-radius: 6px;
  padding: 0.5rem 0.75rem;
  font-size: 1rem;
}

.form-control:focus {
  border-color: #80bdff;
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

.text-danger {
  font-size: 0.9rem;
}

.modal-footer {
  border-top: 1px solid #e9ecef;
  padding: 1rem;
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
  background-color: #007bff;
  border-color: #007bff;
}

.btn-primary:hover {
  background-color: #0056b3;
  border-color: #0056b3;
}

.btn-primary:disabled {
  background-color: #6c757d;
  border-color: #6c757d;
}
</style>
