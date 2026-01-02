<template>
  <div
    class="modal fade"
    :id="modalId"
    tabindex="-1"
    :aria-labelledby="`${modalId}Label`"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-fullscreen-lg-down modal-xl">
      <div class="modal-content organization-modal">
        <!-- Modal Header -->
        <div class="modal-header border-bottom">
          <div class="d-flex align-items-center">
            <i class="bi bi-building text-primary me-3" style="font-size: 1.5rem"></i>
            <div>
              <h4 class="modal-title mb-0" :id="`${modalId}Label`">Edit Organization</h4>
              <small class="text-muted">{{ formData.name || 'Organization Details' }}</small>
            </div>
          </div>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>

        <!-- Modal Body with Tabs -->
        <div class="modal-body p-0">
          <!-- Tab Navigation -->
          <ul class="nav nav-tabs organization-tabs" role="tablist">
            <li class="nav-item" role="presentation">
              <button
                class="nav-link active"
                id="details-tab"
                data-bs-toggle="tab"
                data-bs-target="#details"
                type="button"
                role="tab"
                aria-controls="details"
                aria-selected="true"
              >
                <i class="bi bi-info-circle me-2"></i>
                Details
              </button>
            </li>
            <li class="nav-item" role="presentation">
              <button
                class="nav-link"
                id="resources-tab"
                data-bs-toggle="tab"
                data-bs-target="#resources"
                type="button"
                role="tab"
                aria-controls="resources"
                aria-selected="false"
                @click="loadResources"
              >
                <i class="bi bi-database me-2"></i>
                Resources
                <UiBadge v-if="resourcesCount > 0" color="bg-secondary ms-2">{{
                  resourcesCount
                }}</UiBadge>
              </button>
            </li>
          </ul>

          <!-- Tab Content -->
          <div class="tab-content">
            <!-- Organization Details Tab -->
            <div
              class="tab-pane fade show active p-4"
              id="details"
              role="tabpanel"
              aria-labelledby="details-tab"
            >
              <form @submit.prevent="handleSubmit" class="organization-form">
                <div class="row">
                  <div class="col-md-6">
                    <div class="mb-3">
                      <label for="organizationName" class="form-label fw-medium">
                        Organization Name <span class="text-danger">*</span>
                      </label>
                      <input
                        id="organizationName"
                        v-model="formData.name"
                        type="text"
                        class="form-control"
                        :class="{ 'is-invalid': errors.name }"
                        required
                        placeholder="Enter organization name"
                      />
                      <div v-if="errors.name" class="invalid-feedback">{{ errors.name }}</div>
                    </div>
                  </div>

                  <div class="col-md-6">
                    <div class="mb-3">
                      <label for="externalId" class="form-label fw-medium">
                        External ID <span class="text-danger">*</span>
                      </label>
                      <input
                        id="externalId"
                        v-model="formData.externalId"
                        type="text"
                        class="form-control"
                        :class="{ 'is-invalid': errors.externalId }"
                        required
                        placeholder="Enter external identifier"
                      />
                      <div v-if="errors.externalId" class="invalid-feedback">
                        {{ errors.externalId }}
                      </div>
                    </div>
                  </div>
                </div>

                <div class="mb-3">
                  <label for="organizationDescription" class="form-label fw-medium"
                    >Description</label
                  >
                  <textarea
                    id="organizationDescription"
                    v-model="formData.description"
                    class="form-control"
                    :class="{ 'is-invalid': errors.description }"
                    rows="4"
                    placeholder="Enter organization description..."
                  ></textarea>
                  <div v-if="errors.description" class="invalid-feedback">
                    {{ errors.description }}
                  </div>
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
                      <div v-if="errors.contactEmail" class="invalid-feedback">
                        {{ errors.contactEmail }}
                      </div>
                    </div>
                  </div>

                  <div class="col-md-6">
                    <div class="mb-3">
                      <label for="organizationUri" class="form-label fw-medium"
                        >Organization URI</label
                      >
                      <input
                        id="organizationUri"
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
                      id="organizationWithdrawn"
                      v-model="formData.withdrawn"
                      type="checkbox"
                      class="form-check-input"
                    />
                    <label for="organizationWithdrawn" class="form-check-label fw-medium">
                      Organization is withdrawn (inactive)
                    </label>
                  </div>
                  <small class="form-text text-muted">
                    Check this box if the organization is no longer active or available
                  </small>
                </div>
              </form>
            </div>

            <!-- Resources Tab -->
            <div
              class="tab-pane fade"
              id="resources"
              role="tabpanel"
              aria-labelledby="resources-tab"
            >
              <div v-if="loadingResources" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">Loading resources...</span>
                </div>
                <p class="text-muted mt-2">Loading organization resources...</p>
              </div>
              <ResourcesViewer
                v-else-if="organizationWithResources && props.organization"
                :organization="organizationWithResources"
                :loading="loadingResources"
              />
              <div v-else class="text-center py-5">
                <div class="text-muted">
                  <i class="bi bi-exclamation-triangle me-2"></i>
                  Failed to load organization resources
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Modal Footer -->
        <div class="modal-footer border-top bg-light">
          <button type="button" class="btn btn-dark" data-bs-dismiss="modal">
            <i class="bi bi-x-circle me-2"></i>Cancel
          </button>
          <button type="button" class="btn btn-secondary" @click="handleSubmit" :disabled="saving">
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
import { useOrganizationsStore } from '@/store/organizations'
import ResourcesViewer from '@/components/governance/ResourcesViewer.vue'
import UiBadge from '@/components/ui/UiBadge.vue'

const props = defineProps({
  modalId: {
    type: String,
    required: true,
    default: 'editOrganizationModal',
  },
  organization: {
    type: Object,
    default: null,
  },
  shown: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update', 'close'])

const organizationsStore = useOrganizationsStore()

const formData = ref({
  name: '',
  externalId: '',
  description: '',
  contactEmail: '',
  uri: '',
  withdrawn: false,
})

const errors = ref({})
const saving = ref(false)
const resourcesCount = ref(0)

// Watch for organization changes to populate form
watch(
  () => props.organization,
  async (newOrganization) => {
    if (newOrganization) {
      formData.value = {
        name: newOrganization.name || '',
        externalId: newOrganization.externalId || '',
        description: newOrganization.description || '',
        contactEmail: newOrganization.contactEmail || '',
        uri: newOrganization.uri || '',
        withdrawn: newOrganization.withdrawn || false,
      }
      errors.value = {}
      // Fetch organization with resources when modal opens
      await loadOrganizationWithResources(newOrganization.id)
    }
  },
  { immediate: true },
)

const organizationWithResources = ref(null)
const loadingResources = ref(false)

const loadOrganizationWithResources = async (organizationId) => {
  try {
    loadingResources.value = true
    // Fetch organization with expanded resources using the existing organizations store
    const response = await organizationsStore.getOrganizationById(organizationId, 'resources')
    organizationWithResources.value = response
    // Update resources count for the badge
    resourcesCount.value = response.resources?.length || 0
  } catch (error) {
    console.error('Error loading organization with resources:', error)
    organizationWithResources.value = null
  } finally {
    loadingResources.value = false
  }
  console.log(resourcesCount)
}

const validateForm = () => {
  errors.value = {}

  if (!formData.value.name?.trim()) {
    errors.value.name = 'Organization name is required'
  }

  if (!formData.value.externalId?.trim()) {
    errors.value.externalId = 'External ID is required'
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
  if (!validateForm() || !props.organization) {
    return
  }

  saving.value = true

  try {
    const updateData = {
      name: formData.value.name.trim(),
      externalId: formData.value.externalId.trim(),
      description: formData.value.description?.trim() || '',
      contactEmail: formData.value.contactEmail?.trim() || '',
      uri: formData.value.uri?.trim() || '',
      withdrawn: formData.value.withdrawn,
    }

    emit('update', {
      organizationId: props.organization.id,
      updateData,
    })
  } catch (error) {
    console.error('Error submitting form:', error)
  } finally {
    saving.value = false
  }
}

const loadResources = () => {
  // This will be called when the resources tab is clicked
  // The ResourcesManagement component will handle the actual loading
}
</script>

<style scoped>
.organization-modal {
  height: 90vh;
  max-height: 900px;
}

.organization-tabs {
  background-color: #f8f9fa;
  border-bottom: 1px solid #e8ecef;
  margin-bottom: 0;
  padding: 0 1rem;
}

.organization-tabs .nav-link {
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

.organization-tabs .nav-link:hover {
  color: #495057;
  background-color: rgba(13, 110, 253, 0.1);
  border-bottom-color: #0d6efd;
}

.organization-tabs .nav-link.active {
  color: #0d6efd;
  background-color: #ffffff;
  border-bottom-color: #0d6efd;
  font-weight: 600;
}

.organization-form {
  max-width: 800px;
}

.form-label.fw-medium {
  font-weight: 500;
  color: #495057;
  margin-bottom: 0.5rem;
}

.form-control {
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
  padding: 0.75rem;
  font-size: 0.95rem;
  transition:
    border-color 0.15s ease-in-out,
    box-shadow 0.15s ease-in-out;
}

.form-control:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.form-check-input:checked {
  background-color: #0d6efd;
  border-color: #0d6efd;
}

.badge {
  font-size: 0.75rem;
  border-radius: 10px;
}

.modal-header {
  padding: 1.5rem;
  background-color: #ffffff;
}

.modal-footer {
  padding: 1rem 1.5rem;
}

.btn {
  font-weight: 500;
  border-radius: 0.375rem;
  padding: 0.5rem 1rem;
}

.btn-primary {
  background-color: #0d6efd;
  border-color: #0d6efd;
}

.btn-outline-secondary {
  color: #6c757d;
  border-color: #6c757d;
}

.btn-outline-secondary:hover {
  background-color: #6c757d;
  border-color: #6c757d;
}

@media (max-width: 768px) {
  .organization-modal {
    height: 100vh;
    max-height: none;
  }

  .organization-tabs {
    padding: 0;
  }

  .organization-tabs .nav-link {
    padding: 0.75rem 1rem;
    font-size: 0.875rem;
  }

  .modal-header {
    padding: 1rem;
  }

  .tab-pane#details {
    padding: 1rem !important;
  }
}
</style>
