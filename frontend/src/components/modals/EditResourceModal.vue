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
          <h5 class="modal-title" :id="`${modalId}Label`">Edit Resource</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit">
            <div class="mb-3">
              <label for="resourceName" class="form-label">Resource Name *</label>
              <input
                id="resourceName"
                v-model="formData.name"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.name }"
                required
              />
              <div v-if="errors.name" class="invalid-feedback">{{ errors.name }}</div>
            </div>

            <div class="mb-3">
              <label for="resourceDescription" class="form-label">Description</label>
              <textarea
                id="resourceDescription"
                v-model="formData.description"
                class="form-control"
                :class="{ 'is-invalid': errors.description }"
                rows="3"
                placeholder="Enter resource description..."
              ></textarea>
              <div v-if="errors.description" class="invalid-feedback">{{ errors.description }}</div>
            </div>

            <div class="mb-3">
              <label for="contactEmail" class="form-label">Contact Email</label>
              <input
                id="contactEmail"
                v-model="formData.contactEmail"
                type="email"
                class="form-control"
                :class="{ 'is-invalid': errors.contactEmail }"
                placeholder="contact@example.com"
              />
              <div v-if="errors.contactEmail" class="invalid-feedback">
                {{ errors.contactEmail }}
              </div>
            </div>

            <div class="mb-3">
              <label for="resourceUri" class="form-label">Resource URI</label>
              <input
                id="resourceUri"
                v-model="formData.uri"
                type="url"
                class="form-control"
                :class="{ 'is-invalid': errors.uri }"
                placeholder="https://example.com"
              />
              <div v-if="errors.uri" class="invalid-feedback">{{ errors.uri }}</div>
            </div>

            <div class="mb-3">
              <div class="form-check">
                <input
                  id="resourceWithdrawn"
                  v-model="formData.withdrawn"
                  type="checkbox"
                  class="form-check-input"
                />
                <label for="resourceWithdrawn" class="form-check-label">
                  Resource is withdrawn (inactive)
                </label>
              </div>
              <small class="form-text text-muted">
                Check this box if the resource is no longer active or available
              </small>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
          <button type="button" class="btn btn-primary" @click="handleSubmit" :disabled="saving">
            <span v-if="saving" class="spinner-border spinner-border-sm me-2" role="status">
              <span class="visually-hidden">Loading...</span>
            </span>
            {{ saving ? 'Saving...' : 'Save Changes' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modalId: {
    type: String,
    required: true,
    default: 'editResourceModal',
  },
  resource: {
    type: Object,
    default: null,
  },
  shown: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update', 'close'])

const formData = ref({
  name: '',
  description: '',
  contactEmail: '',
  uri: '',
  withdrawn: false,
})

const errors = ref({})
const saving = ref(false)

// Watch for resource changes to populate form
watch(
  () => props.resource,
  (newResource) => {
    if (newResource) {
      formData.value = {
        name: newResource.name || '',
        description: newResource.description || '',
        contactEmail: newResource.contactEmail || '',
        uri: newResource.uri || '',
        withdrawn: newResource.withdrawn || false,
      }
      // Clear any previous errors
      errors.value = {}
    }
  },
  { immediate: true },
)

const validateForm = () => {
  errors.value = {}

  if (!formData.value.name?.trim()) {
    errors.value.name = 'Resource name is required'
  }

  if (formData.value.contactEmail && !isValidEmail(formData.value.contactEmail)) {
    errors.value.contactEmail = 'Please enter a valid email address'
  }

  if (formData.value.uri && !isValidUrl(formData.value.uri)) {
    errors.value.uri = 'Please enter a valid URL'
  }

  return Object.keys(errors.value).length === 0
}

const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

const isValidUrl = (url) => {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  saving.value = true

  try {
    // Create update payload with only changed fields
    const updateData = {}

    if (formData.value.name !== props.resource?.name) {
      updateData.name = formData.value.name
    }
    if (formData.value.description !== props.resource?.description) {
      updateData.description = formData.value.description
    }
    if (formData.value.contactEmail !== props.resource?.contactEmail) {
      updateData.contactEmail = formData.value.contactEmail
    }
    if (formData.value.uri !== props.resource?.uri) {
      updateData.uri = formData.value.uri
    }
    if (formData.value.withdrawn !== props.resource?.withdrawn) {
      updateData.withdrawn = formData.value.withdrawn
    }

    // Only emit update if there are actual changes
    if (Object.keys(updateData).length > 0) {
      emit('update', { resourceId: props.resource.id, updateData })
    } else {
      emit('close')
    }
  } catch (error) {
    console.error('Error submitting form:', error)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.modal-body {
  max-height: 70vh;
  overflow-y: auto;
}

.form-label {
  font-weight: 600;
  color: #495057;
}

.form-control:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.is-invalid {
  border-color: #dc3545;
}

.invalid-feedback {
  display: block;
  width: 100%;
  margin-top: 0.25rem;
  font-size: 0.875rem;
  color: #dc3545;
}

.form-check-input:checked {
  background-color: #0d6efd;
  border-color: #0d6efd;
}

.spinner-border-sm {
  width: 1rem;
  height: 1rem;
}
</style>
