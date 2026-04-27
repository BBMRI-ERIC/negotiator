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
        <!-- Modal Header -->
        <div class="modal-header">
          <h5 class="modal-title" :id="`${modalId}Label`">Create Resource</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
            @click="handleClose"
          ></button>
        </div>

        <!-- Modal Body -->
        <div class="modal-body">
          <form @submit.prevent="handleSubmit" novalidate>
            <div class="mb-3">
              <label for="resourceName" class="form-label">
                Name <span class="text-danger">*</span>
              </label>
              <input
                id="resourceName"
                v-model="formData.name"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.name }"
                placeholder="Enter resource name"
                required
              />
              <div v-if="errors.name" class="invalid-feedback">{{ errors.name }}</div>
            </div>

            <div class="mb-3">
              <label for="sourceId" class="form-label">
                Source ID <span class="text-danger">*</span>
              </label>
              <input
                id="sourceId"
                v-model="formData.sourceId"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.sourceId }"
                placeholder="Enter source identifier"
                required
              />
              <div v-if="errors.sourceId" class="invalid-feedback">{{ errors.sourceId }}</div>
            </div>

            <div class="mb-3">
              <label for="resourceDescription" class="form-label">
                Description <span class="text-danger">*</span>
              </label>
              <textarea
                id="resourceDescription"
                v-model="formData.description"
                class="form-control"
                :class="{ 'is-invalid': errors.description }"
                rows="3"
                placeholder="Enter resource description"
                required
              ></textarea>
              <div v-if="errors.description" class="invalid-feedback">{{ errors.description }}</div>
            </div>

            <div class="mb-3">
              <label for="accessForm" class="form-label">
                Access Form <span class="text-danger">*</span>
              </label>
              <select
                id="accessForm"
                v-model="formData.accessFormId"
                class="form-select"
                :class="{ 'is-invalid': errors.accessFormId }"
                required
              >
                <option value="">Select an access form</option>
                <option v-for="form in accessForms" :key="form.id" :value="form.id">
                  {{ form.name }}
                </option>
              </select>
              <div v-if="errors.accessFormId" class="invalid-feedback">
                {{ errors.accessFormId }}
              </div>
            </div>

            <div class="mb-3">
              <label for="discoveryService" class="form-label">
                Discovery Service ID <span class="text-danger">*</span>
              </label>
              <input
                id="discoveryService"
                v-model="formData.discoveryServiceId"
                type="number"
                class="form-control"
                :class="{ 'is-invalid': errors.discoveryServiceId }"
                placeholder="Enter discovery service ID"
                required
                min="1"
              />
              <div v-if="errors.discoveryServiceId" class="invalid-feedback">
                {{ errors.discoveryServiceId }}
              </div>
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
              <label class="form-label">Organization</label>
              <div class="form-control-plaintext bg-light p-2 rounded border">
                <div class="fw-medium">{{ organizationName }}</div>
                <small class="text-muted">ID: {{ organizationId }}</small>
              </div>
            </div>
          </form>
        </div>

        <!-- Modal Footer -->
        <div class="modal-footer">
          <button
            type="button"
            class="btn btn-secondary"
            data-bs-dismiss="modal"
            @click="handleClose"
            :disabled="submitting"
          >
            Cancel
          </button>
          <button
            type="button"
            class="btn btn-primary"
            @click="handleSubmit"
            :disabled="submitting || !isFormValid"
          >
            <span
              v-if="submitting"
              class="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            ></span>
            {{ submitting ? 'Creating...' : 'Create Resource' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useResourcesStore } from '@/store/resources'
import { useFormsStore } from '@/store/forms'

const props = defineProps({
  modalId: {
    type: String,
    required: true,
  },
  organizationId: {
    type: [String, Number],
    required: true,
  },
  organizationName: {
    type: String,
    default: '',
  },
  shown: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['create', 'close'])

const resourcesStore = useResourcesStore()
const formsStore = useFormsStore()

// Form data
const formData = ref({
  name: '',
  description: '',
  sourceId: '',
  organizationId: Number(props.organizationId),
  accessFormId: '',
  discoveryServiceId: '',
  contactEmail: '',
  uri: '',
})

// Form state
const submitting = ref(false)
const errors = ref({})
const accessForms = ref([])

// Computed properties
const isFormValid = computed(() => {
  // Check required fields with proper validation for each type
  const hasName = formData.value.name?.trim().length > 0
  const hasDescription = formData.value.description?.trim().length > 0
  const hasSourceId = formData.value.sourceId?.trim().length > 0
  const hasAccessForm = formData.value.accessFormId && formData.value.accessFormId !== ''
  const hasDiscoveryService =
    formData.value.discoveryServiceId &&
    formData.value.discoveryServiceId !== '' &&
    !isNaN(Number(formData.value.discoveryServiceId))

  return hasName && hasDescription && hasSourceId && hasAccessForm && hasDiscoveryService
})

// Watch for organization ID changes
watch(
  () => props.organizationId,
  (newId) => {
    formData.value.organizationId = Number(newId)
  },
)

// Load access forms on mount
onMounted(async () => {
  try {
    const forms = await formsStore.retrieveAllAccessForms()
    accessForms.value = forms || []
  } catch (error) {
    console.error('Error loading access forms:', error)
    accessForms.value = []
  }
})

// Validation
const validateForm = () => {
  errors.value = {}

  // Required field validation
  if (!formData.value.name?.trim()) {
    errors.value.name = 'Name is required'
  }

  if (!formData.value.description?.trim()) {
    errors.value.description = 'Description is required'
  }

  if (!formData.value.sourceId?.trim()) {
    errors.value.sourceId = 'Source ID is required'
  }

  if (!formData.value.accessFormId) {
    errors.value.accessFormId = 'Access form is required'
  }

  if (!formData.value.discoveryServiceId) {
    errors.value.discoveryServiceId = 'Discovery service ID is required'
  } else if (isNaN(formData.value.discoveryServiceId) || formData.value.discoveryServiceId < 1) {
    errors.value.discoveryServiceId = 'Discovery service ID must be a positive number'
  }

  // Email validation
  if (formData.value.contactEmail && !isValidEmail(formData.value.contactEmail)) {
    errors.value.contactEmail = 'Please enter a valid email address'
  }

  // URI validation
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

const resetForm = () => {
  formData.value = {
    name: '',
    description: '',
    sourceId: '',
    organizationId: Number(props.organizationId),
    accessFormId: '',
    discoveryServiceId: '',
    contactEmail: '',
    uri: '',
  }
  errors.value = {}
  submitting.value = false
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  submitting.value = true

  try {
    // Prepare the resource data for API call
    // The API expects an array of resources
    const resourceData = [
      {
        name: formData.value.name.trim(),
        description: formData.value.description.trim(),
        sourceId: formData.value.sourceId.trim(),
        organizationId: formData.value.organizationId,
        accessFormId: Number(formData.value.accessFormId),
        discoveryServiceId: Number(formData.value.discoveryServiceId),
        contactEmail: formData.value.contactEmail?.trim() || null,
        uri: formData.value.uri?.trim() || null,
      },
    ]

    console.log('Creating resource with data:', resourceData)

    const result = await resourcesStore.createResources(resourceData)

    console.log('Resource created successfully:', result)

    // Emit success event with created resource data
    emit('create', result)

    // Reset form and close modal
    resetForm()
  } catch (error) {
    console.error('Error creating resource:', error)

    // Handle validation errors from server
    if (error.response?.status === 400 && error.response?.data?.violations) {
      const violations = error.response.data.violations
      violations.forEach((violation) => {
        errors.value[violation.field] = violation.message
      })
    }
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  resetForm()
  emit('close')
}

// Reset form when modal is shown
watch(
  () => props.shown,
  (isShown) => {
    if (isShown) {
      resetForm()
    }
  },
)
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
