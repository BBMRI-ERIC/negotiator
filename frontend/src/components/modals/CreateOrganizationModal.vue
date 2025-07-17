<template>
  <div
    class="modal fade"
    :id="modalId"
    tabindex="-1"
    :aria-labelledby="`${modalId}Label`"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-lg">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" :id="`${modalId}Label`">Create New Organization</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit">
            <div class="mb-3">
              <label for="createOrganizationName" class="form-label">Organization Name *</label>
              <input
                id="createOrganizationName"
                v-model="formData.name"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.name }"
                required
              />
              <div v-if="errors.name" class="invalid-feedback">{{ errors.name }}</div>
            </div>

            <div class="mb-3">
              <label for="createExternalId" class="form-label">External ID *</label>
              <input
                id="createExternalId"
                v-model="formData.externalId"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.externalId }"
                required
              />
              <div v-if="errors.externalId" class="invalid-feedback">{{ errors.externalId }}</div>
            </div>

            <div class="mb-3">
              <label for="createOrganizationDescription" class="form-label">Description *</label>
              <textarea
                id="createOrganizationDescription"
                v-model="formData.description"
                class="form-control"
                :class="{ 'is-invalid': errors.description }"
                rows="3"
                placeholder="Enter organization description..."
                required
              ></textarea>
              <div v-if="errors.description" class="invalid-feedback">{{ errors.description }}</div>
            </div>

            <div class="mb-3">
              <label for="createContactEmail" class="form-label">Contact Email</label>
              <input
                id="createContactEmail"
                v-model="formData.contactEmail"
                type="email"
                class="form-control"
                :class="{ 'is-invalid': errors.contactEmail }"
                placeholder="contact@example.com"
              />
              <div v-if="errors.contactEmail" class="invalid-feedback">{{ errors.contactEmail }}</div>
            </div>

            <div class="mb-3">
              <label for="createOrganizationUri" class="form-label">Organization URI</label>
              <input
                id="createOrganizationUri"
                v-model="formData.uri"
                type="url"
                class="form-control"
                :class="{ 'is-invalid': errors.uri }"
                placeholder="https://example.com"
              />
              <div v-if="errors.uri" class="invalid-feedback">{{ errors.uri }}</div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            @click="handleSubmit"
            :disabled="isSubmitting"
          >
            <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-2" role="status"></span>
            {{ isSubmitting ? 'Creating...' : 'Create Organization' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'

const props = defineProps({
  modalId: {
    type: String,
    required: true
  },
  shown: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['create', 'close'])

const isSubmitting = ref(false)
const errors = ref({})

const formData = reactive({
  name: '',
  externalId: '',
  description: '',
  contactEmail: '',
  uri: '',
  withdrawn: false
})

// Reset form when modal is closed
watch(() => props.shown, (newShown) => {
  if (!newShown) {
    resetForm()
  }
})

const resetForm = () => {
  formData.name = ''
  formData.externalId = ''
  formData.description = ''
  formData.contactEmail = ''
  formData.uri = ''
  formData.withdrawn = false
  errors.value = {}
  isSubmitting.value = false
}

const validateForm = () => {
  errors.value = {}

  if (!formData.name?.trim()) {
    errors.value.name = 'Organization name is required'
  }

  if (!formData.externalId?.trim()) {
    errors.value.externalId = 'External ID is required'
  }

  if (!formData.description?.trim()) {
    errors.value.description = 'Description is required'
  }

  if (formData.contactEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.contactEmail)) {
    errors.value.contactEmail = 'Please enter a valid email address'
  }

  if (formData.uri && !/^https?:\/\/.+/.test(formData.uri)) {
    errors.value.uri = 'Please enter a valid URL (must start with http:// or https://)'
  }

  return Object.keys(errors.value).length === 0
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  isSubmitting.value = true

  try {
    const createData = {
      name: formData.name.trim(),
      externalId: formData.externalId.trim(),
      description: formData.description?.trim() || null,
      contactEmail: formData.contactEmail?.trim() || null,
      uri: formData.uri?.trim() || null,
      withdrawn: formData.withdrawn
    }

    emit('create', createData)
  } catch (error) {
    console.error('Error in handleSubmit:', error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.modal-header {
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
}

.modal-title {
  color: #495057;
  font-weight: 600;
}

.form-label {
  font-weight: 500;
  color: #495057;
}

.form-control:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.btn-primary {
  background-color: #0d6efd;
  border-color: #0d6efd;
}

.btn-primary:hover {
  background-color: #0b5ed7;
  border-color: #0a58ca;
}

.invalid-feedback {
  display: block;
}
</style>
