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
        <div class="modal-header border-bottom">
          <div class="d-flex align-items-center">
            <i class="bi bi-plus-circle text-success me-3" style="font-size: 1.5rem;"></i>
            <div>
              <h4 class="modal-title mb-0" :id="`${modalId}Label`">
                Create Resource
              </h4>
              <small class="text-muted">{{ organizationName || 'Add new resource' }}</small>
            </div>
          </div>
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
            <!-- Basic Information -->
            <div class="mb-4">
              <h6 class="text-uppercase text-muted fw-bold mb-3">Basic Information</h6>

              <div class="row">
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="resourceName" class="form-label fw-medium">
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
                </div>

                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="sourceId" class="form-label fw-medium">
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
                </div>
              </div>

              <div class="mb-3">
                <label for="resourceDescription" class="form-label fw-medium">
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
            </div>

            <!-- Contact Information -->
            <div class="mb-4">
              <h6 class="text-uppercase text-muted fw-bold mb-3">Contact Information</h6>

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
            </div>

            <!-- Configuration -->
            <div class="mb-4">
              <h6 class="text-uppercase text-muted fw-bold mb-3">Configuration</h6>

              <div class="row">
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="accessForm" class="form-label fw-medium">
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
                      <option
                        v-for="form in accessForms"
                        :key="form.id"
                        :value="form.id"
                      >
                        {{ form.name }}
                      </option>
                    </select>
                    <div v-if="errors.accessFormId" class="invalid-feedback">{{ errors.accessFormId }}</div>
                  </div>
                </div>

                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="discoveryService" class="form-label fw-medium">
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
                    <div v-if="errors.discoveryServiceId" class="invalid-feedback">{{ errors.discoveryServiceId }}</div>
                    <small class="form-text text-muted">Numeric ID of the discovery service</small>
                  </div>
                </div>
              </div>
            </div>

            <!-- Organization Info (Read-only) -->
            <div class="mb-3">
              <label class="form-label fw-medium">Organization</label>
              <div class="form-control-plaintext bg-light p-3 rounded">
                <div class="fw-medium">{{ organizationName }}</div>
                <small class="text-muted">ID: {{ organizationId }}</small>
              </div>
              <small class="form-text text-muted">This resource will be assigned to this organization</small>
            </div>
          </form>
        </div>

        <!-- Modal Footer -->
        <div class="modal-footer border-top">
          <button
            type="button"
            class="btn btn-outline-secondary"
            data-bs-dismiss="modal"
            @click="handleClose"
            :disabled="submitting"
          >
            Cancel
          </button>
          <button
            type="button"
            class="btn btn-success"
            @click="handleSubmit"
            :disabled="submitting || !isFormValid"
          >
            <span
              v-if="submitting"
              class="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            ></span>
            <i v-else class="bi bi-plus-circle me-2"></i>
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
    required: true
  },
  organizationId: {
    type: [String, Number],
    required: true
  },
  organizationName: {
    type: String,
    default: ''
  },
  shown: {
    type: Boolean,
    default: false
  }
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
  uri: ''
})

// Form state
const submitting = ref(false)
const errors = ref({})
const accessForms = ref([])

// Computed properties
const isFormValid = computed(() => {
  return formData.value.name &&
         formData.value.description &&
         formData.value.sourceId &&
         formData.value.accessFormId &&
         formData.value.discoveryServiceId &&
         Object.keys(errors.value).length === 0
})

// Watch for organization ID changes
watch(() => props.organizationId, (newId) => {
  formData.value.organizationId = Number(newId)
})

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
    uri: ''
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
    const resourceData = [{
      name: formData.value.name.trim(),
      description: formData.value.description.trim(),
      sourceId: formData.value.sourceId.trim(),
      organizationId: formData.value.organizationId,
      accessFormId: Number(formData.value.accessFormId),
      discoveryServiceId: Number(formData.value.discoveryServiceId),
      contactEmail: formData.value.contactEmail?.trim() || null,
      uri: formData.value.uri?.trim() || null
    }]

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
      violations.forEach(violation => {
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
watch(() => props.shown, (isShown) => {
  if (isShown) {
    resetForm()
  }
})
</script>

<style scoped>
.modal-content {
  border: none;
  box-shadow: 0 0.5rem 2rem rgba(0, 0, 0, 0.2);
}

.modal-header {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 1.5rem;
}

.modal-body {
  padding: 2rem;
  max-height: 70vh;
  overflow-y: auto;
}

.modal-footer {
  padding: 1.5rem 2rem;
  background: #f8f9fa;
}

.form-label {
  color: #495057;
  margin-bottom: 0.5rem;
}

.form-control, .form-select {
  border: 1px solid #ced4da;
  padding: 0.625rem 0.75rem;
}

.form-control:focus, .form-select:focus {
  border-color: #198754;
  box-shadow: 0 0 0 0.2rem rgba(25, 135, 84, 0.25);
}

.text-danger {
  color: #dc3545 !important;
}

.bg-light {
  background-color: #f8f9fa !important;
}

h6.text-uppercase {
  font-size: 0.875rem;
  letter-spacing: 0.05em;
  border-bottom: 2px solid #e9ecef;
  padding-bottom: 0.5rem;
}

.form-control-plaintext {
  border: 1px solid #e9ecef;
}

.btn-success:disabled {
  opacity: 0.65;
}
</style>
