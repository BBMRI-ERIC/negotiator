<template>
  <div
    class="modal fade"
    :id="modalId"
    tabindex="-1"
    :aria-labelledby="`${modalId}Label`"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-lg">
      <div class="modal-content resource-modal">
        <!-- Modal Header -->
        <div class="modal-header border-bottom">
          <div class="d-flex align-items-center">
            <i class="bi bi-database text-primary me-3" style="font-size: 1.5rem;"></i>
            <div>
              <h4 class="modal-title mb-0" :id="`${modalId}Label`">
                Edit Resource
              </h4>
              <small class="text-muted">{{ formData.name || 'Resource Details' }}</small>
            </div>
          </div>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>

        <!-- Modal Body -->
        <div class="modal-body p-4">
          <!-- General Error Alert -->
          <div v-if="errors.general" class="alert alert-danger mb-3" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i>
            {{ errors.general }}
          </div>

          <!-- Resource Details Form -->
          <form @submit.prevent="handleSubmit" class="resource-form">
            <div class="row">
              <div class="col-md-6">
                <div class="mb-3">
                  <label for="resourceName" class="form-label fw-medium">
                    Resource Name <span class="text-danger">*</span>
                  </label>
                  <input
                    id="resourceName"
                    v-model="formData.name"
                    type="text"
                    class="form-control"
                    :class="{ 'is-invalid': errors.name }"
                    required
                    placeholder="Enter resource name"
                  />
                  <div v-if="errors.name" class="invalid-feedback">{{ errors.name }}</div>
                </div>
              </div>

              <div class="col-md-6">
                <div class="mb-3">
                  <label for="sourceId" class="form-label fw-medium">
                    Source ID
                  </label>
                  <input
                    id="sourceId"
                    :value="resource?.sourceId || ''"
                    type="text"
                    class="form-control"
                    readonly
                    disabled
                    placeholder="Source ID (read-only)"
                  />
                  <small class="form-text text-muted">Source ID cannot be modified</small>
                </div>
              </div>
            </div>

            <div class="mb-3">
              <label for="resourceDescription" class="form-label fw-medium">Description</label>
              <textarea
                id="resourceDescription"
                v-model="formData.description"
                class="form-control"
                :class="{ 'is-invalid': errors.description }"
                rows="4"
                placeholder="Enter resource description..."
              ></textarea>
              <div v-if="errors.description" class="invalid-feedback">{{ errors.description }}</div>
            </div>

            <div class="row">
              <div class="col-md-6">
                <div class="mb-3">
                  <label for="contactEmail" class="form-label fw-medium">Contact Email</label>
                  <input
                    id="contactEmail"
                    v-model="formData.contactEmail"
                    type="email"
                    class="form-control"
                    :class="{ 'is-invalid': errors.contactEmail }"
                    placeholder="contact@example.com"
                  />
                  <div v-if="errors.contactEmail" class="invalid-feedback">{{ errors.contactEmail }}</div>
                </div>
              </div>

              <div class="col-md-6">
                <div class="mb-3">
                  <label for="resourceUri" class="form-label fw-medium">Resource URI</label>
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
              </div>
            </div>

            <div class="mb-3">
              <div class="form-check">
                <input
                  id="resourceWithdrawn"
                  v-model="formData.withdrawn"
                  type="checkbox"
                  class="form-check-input"
                />
                <label for="resourceWithdrawn" class="form-check-label fw-medium">
                  Resource is withdrawn (inactive)
                </label>
              </div>
              <small class="form-text text-muted">
                Check this box if the resource is no longer active or available
              </small>
            </div>
          </form>
        </div>

        <!-- Modal Footer -->
        <div class="modal-footer border-top bg-light">
          <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">
            <i class="bi bi-x-circle me-2"></i>Cancel
          </button>
          <button
            type="button"
            class="btn btn-primary"
            @click="handleSubmit"
            :disabled="saving"
          >
            <span v-if="saving" class="spinner-border spinner-border-sm me-2" role="status">
              <span class="visually-hidden">Loading...</span>
            </span>
            <i v-else class="bi bi-check-circle me-2"></i>
            {{ saving ? 'Saving...' : 'Save Changes' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useResourcesStore } from '@/store/resources.js'

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

const resourcesStore = useResourcesStore()

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
      errors.value = {}
    }
  },
  { immediate: true }
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
  if (!validateForm() || !props.resource) {
    return
  }

  saving.value = true

  try {
    const updateData = {
      name: formData.value.name.trim(),
      description: formData.value.description?.trim() || '',
      contactEmail: formData.value.contactEmail?.trim() || '',
      uri: formData.value.uri?.trim() || '',
      withdrawn: formData.value.withdrawn,
    }

    console.log('Updating resource with ID:', props.resource.id)
    console.log('Update data:', updateData)

    // Call the API to update the resource
    await resourcesStore.updateResource(props.resource.id, updateData)

    console.log('Resource updated successfully')

    // Emit the update event with the updated data
    emit('update', {
      resourceId: props.resource.id,
      updateData,
    })

    // Close the modal after successful update
    emit('close')
  } catch (error) {
    console.error('Error updating resource:', error)

    // Handle validation errors from server
    if (error.response?.status === 400 && error.response?.data?.violations) {
      const violations = error.response.data.violations
      violations.forEach(violation => {
        errors.value[violation.field] = violation.message
      })
    } else if (error.response?.data?.message) {
      // Handle other server errors
      errors.value.general = error.response.data.message
    } else {
      // Handle network or unexpected errors
      errors.value.general = 'An error occurred while updating the resource. Please try again.'
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.resource-modal {
  max-height: 600px;
  height: auto;
}

.modal-body {
  padding: 1.5rem !important;
  max-height: 400px;
  overflow-y: auto;
}

.resource-tabs {
  background-color: #f8f9fa;
  border-bottom: 1px solid #e8ecef;
  margin-bottom: 0;
  padding: 0 1rem;
}

.resource-tabs .nav-link {
  border: none;
  border-radius: 0;
  color: #6c757d;
  font-weight: 500;
  font-size: 0.95rem;
  padding: 1rem 1.5rem;
  transition: all 0.2s ease;
  background: transparent;
  border-bottom: 3px solid transparent;
}

.resource-tabs .nav-link:hover {
  color: #495057;
  background-color: rgba(13, 110, 253, 0.1);
  border-bottom-color: #0d6efd;
}

.resource-tabs .nav-link.active {
  color: #0d6efd;
  background-color: #ffffff;
  border-bottom-color: #0d6efd;
  font-weight: 600;
}

.resource-form {
  max-width: 100%;
}

.form-label {
  font-size: 0.85rem;
  font-weight: 500;
  color: #495057;
  margin-bottom: 0.3rem;
}

.form-control {
  padding: 0.5rem;
  font-size: 0.9rem;
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
}

.form-control:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.form-control:disabled {
  background-color: #f8f9fa;
  color: #6c757d;
}

.form-control-plaintext {
  padding: 0.75rem 0;
  font-size: 0.95rem;
  color: #495057;
}

.form-check-input {
  margin-top: 0.25rem;
}

.form-check-label {
  font-size: 0.95rem;
}

.form-text {
  font-size: 0.75rem;
  color: #6c757d;
}

.invalid-feedback {
  font-size: 0.75rem;
}

.btn {
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
  border-radius: 0.375rem;
  transition: all 0.2s ease;
}

.spinner-border-sm {
  width: 1rem;
  height: 1rem;
}

.modal-header {
  padding: 1rem 1.5rem;
}

.modal-footer {
  padding: 1rem 1.5rem;
}

@media (max-width: 768px) {
  .resource-modal {
    height: 90vh;
    max-height: none;
  }

  .modal-body {
    padding: 1rem !important;
    max-height: none;
  }
}
</style>
