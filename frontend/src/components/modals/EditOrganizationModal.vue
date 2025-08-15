<template>
  <div
    class="modal fade"
    :id="modalId"
    tabindex="-1"
    :aria-labelledby="`${modalId}Label`"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-xl">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" :id="`${modalId}Label`">Edit Organization</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>
        <div class="modal-body">
          <!-- Organization Form -->
          <form @submit.prevent="handleSubmit">
            <div class="mb-3">
              <label for="organizationName" class="form-label">Organization Name *</label>
              <input
                id="organizationName"
                v-model="formData.name"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.name }"
                required
              />
              <div v-if="errors.name" class="invalid-feedback">{{ errors.name }}</div>
            </div>

            <div class="mb-3">
              <label for="externalId" class="form-label">External ID *</label>
              <input
                id="externalId"
                v-model="formData.externalId"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.externalId }"
                required
              />
              <div v-if="errors.externalId" class="invalid-feedback">{{ errors.externalId }}</div>
            </div>

            <div class="mb-3">
              <label for="organizationDescription" class="form-label">Description</label>
              <textarea
                id="organizationDescription"
                v-model="formData.description"
                class="form-control"
                :class="{ 'is-invalid': errors.description }"
                rows="3"
                placeholder="Enter organization description..."
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
              <label for="organizationUri" class="form-label">Organization URI</label>
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

            <div class="mb-3">
              <div class="form-check">
                <input
                  id="organizationWithdrawn"
                  v-model="formData.withdrawn"
                  type="checkbox"
                  class="form-check-input"
                />
                <label for="organizationWithdrawn" class="form-check-label">
                  Organization is withdrawn (inactive)
                </label>
              </div>
              <small class="form-text text-muted">
                Check this box if the organization is no longer active or available
              </small>
            </div>

            <div class="d-flex justify-content-end">
              <button
                type="button"
                class="btn btn-primary"
                @click="handleSubmit"
                :disabled="saving"
              >
                <span v-if="saving" class="spinner-border spinner-border-sm me-2" role="status">
                  <span class="visually-hidden">Loading...</span>
                </span>
                {{ saving ? 'Saving...' : 'Save Changes' }}
              </button>
            </div>
          </form>

          <!-- Resources Section -->
          <hr class="my-4" />
          <div class="d-flex justify-content-between align-items-center mb-3">
            <h6 class="mb-0">Organization Resources</h6>
            <button
              type="button"
              class="btn btn-sm btn-outline-primary"
              @click="toggleResourceForm"
            >
              <i class="fas fa-plus me-1"></i>
              Add Resource
            </button>
          </div>

          <!-- Add Resource Form -->
          <div v-if="showResourceForm" class="card mb-3">
            <div class="card-header">
              <h6 class="mb-0">Add New Resource</h6>
            </div>
            <div class="card-body">
              <form @submit.prevent="handleResourceSubmit">
                <div class="mb-3">
                  <label for="resourceName" class="form-label">Resource Name *</label>
                  <input
                    id="resourceName"
                    v-model="resourceFormData.name"
                    type="text"
                    class="form-control"
                    :class="{ 'is-invalid': resourceErrors.name }"
                    required
                  />
                  <div v-if="resourceErrors.name" class="invalid-feedback">
                    {{ resourceErrors.name }}
                  </div>
                </div>

                <div class="mb-3">
                  <label for="resourceDescription" class="form-label">Description *</label>
                  <textarea
                    id="resourceDescription"
                    v-model="resourceFormData.description"
                    class="form-control"
                    :class="{ 'is-invalid': resourceErrors.description }"
                    rows="3"
                    placeholder="Enter resource description..."
                    required
                  ></textarea>
                  <div v-if="resourceErrors.description" class="invalid-feedback">
                    {{ resourceErrors.description }}
                  </div>
                </div>

                <div class="mb-3">
                  <label for="sourceId" class="form-label">Source ID *</label>
                  <input
                    id="sourceId"
                    v-model="resourceFormData.sourceId"
                    type="text"
                    class="form-control"
                    :class="{ 'is-invalid': resourceErrors.sourceId }"
                    required
                  />
                  <div v-if="resourceErrors.sourceId" class="invalid-feedback">
                    {{ resourceErrors.sourceId }}
                  </div>
                </div>

                <div class="mb-3">
                  <label for="accessFormId" class="form-label">Access Form ID *</label>
                  <input
                    id="accessFormId"
                    v-model.number="resourceFormData.accessFormId"
                    type="number"
                    class="form-control"
                    :class="{ 'is-invalid': resourceErrors.accessFormId }"
                    required
                    min="1"
                  />
                  <div v-if="resourceErrors.accessFormId" class="invalid-feedback">
                    {{ resourceErrors.accessFormId }}
                  </div>
                </div>

                <div class="mb-3">
                  <label for="discoveryServiceId" class="form-label">Discovery Service ID *</label>
                  <input
                    id="discoveryServiceId"
                    v-model.number="resourceFormData.discoveryServiceId"
                    type="number"
                    class="form-control"
                    :class="{ 'is-invalid': resourceErrors.discoveryServiceId }"
                    required
                    min="1"
                  />
                  <div v-if="resourceErrors.discoveryServiceId" class="invalid-feedback">
                    {{ resourceErrors.discoveryServiceId }}
                  </div>
                </div>

                <div class="mb-3">
                  <label for="resourceContactEmail" class="form-label">Contact Email</label>
                  <input
                    id="resourceContactEmail"
                    v-model="resourceFormData.contactEmail"
                    type="email"
                    class="form-control"
                    :class="{ 'is-invalid': resourceErrors.contactEmail }"
                    placeholder="contact@example.com"
                  />
                  <div v-if="resourceErrors.contactEmail" class="invalid-feedback">
                    {{ resourceErrors.contactEmail }}
                  </div>
                </div>

                <div class="mb-3">
                  <label for="resourceUri" class="form-label">Resource URI</label>
                  <input
                    id="resourceUri"
                    v-model="resourceFormData.uri"
                    type="url"
                    class="form-control"
                    :class="{ 'is-invalid': resourceErrors.uri }"
                    placeholder="https://example.com"
                  />
                  <div v-if="resourceErrors.uri" class="invalid-feedback">
                    {{ resourceErrors.uri }}
                  </div>
                </div>

                <div class="d-flex gap-2">
                  <button type="submit" class="btn btn-primary btn-sm" :disabled="savingResource">
                    <span
                      v-if="savingResource"
                      class="spinner-border spinner-border-sm me-2"
                      role="status"
                    >
                      <span class="visually-hidden">Loading...</span>
                    </span>
                    {{ savingResource ? 'Adding...' : 'Add Resource' }}
                  </button>
                  <button
                    type="button"
                    class="btn btn-secondary btn-sm"
                    @click="cancelResourceForm"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>

          <!-- Existing Resources List -->
          <div v-if="loading" class="text-center py-3">
            <div class="spinner-border" role="status">
              <span class="visually-hidden">Loading resources...</span>
            </div>
          </div>

          <div
            v-else-if="organizationWithResources?.resources?.length > 0"
            class="table-responsive"
          >
            <table class="table table-sm">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Source ID</th>
                  <th>Description</th>
                  <th>Contact Email</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="resource in organizationWithResources.resources" :key="resource.id">
                  <td>{{ resource.name }}</td>
                  <td>{{ resource.sourceId }}</td>
                  <td>{{ resource.description }}</td>
                  <td>{{ resource.contactEmail || '-' }}</td>
                  <td>
                    <button class="btn btn-sm btn-outline-secondary" title="Edit Resource">
                      <i class="fas fa-edit"></i>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else-if="!loading" class="text-center py-3 text-muted">
            <i class="fas fa-inbox fa-2x mb-2"></i>
            <p class="mb-0">No resources found for this organization</p>
          </div>
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
import { useOrganizationsStore } from '../../store/organizations'
import { useResourcesStore } from '../../store/resources'

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

const emit = defineEmits(['update', 'close', 'resourceAdded'])

// Store instances
const organizationsStore = useOrganizationsStore()
const resourcesStore = useResourcesStore()

const formData = ref({
  name: '',
  externalId: '',
  description: '',
  contactEmail: '',
  uri: '',
  withdrawn: false,
})

const resourceFormData = ref({
  name: '',
  description: '',
  sourceId: '',
  accessFormId: null,
  discoveryServiceId: null,
  contactEmail: '',
  uri: '',
})

const errors = ref({})
const resourceErrors = ref({})
const saving = ref(false)
const savingResource = ref(false)
const loading = ref(false)
const showResourceForm = ref(false)
const organizationWithResources = ref(null)

// Watch for organization changes to populate form and fetch resources
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

      // Fetch organization with resources
      await fetchOrganizationWithResources(newOrganization.id)
    }
  },
  { immediate: true },
)

const fetchOrganizationWithResources = async (organizationId) => {
  if (!organizationId) return

  try {
    loading.value = true
    organizationWithResources.value = await organizationsStore.getOrganizationById(
      organizationId,
      'resources',
    )
  } catch (error) {
    console.error('Error fetching organization with resources:', error)
  } finally {
    loading.value = false
  }
}

const toggleResourceForm = () => {
  showResourceForm.value = !showResourceForm.value
  if (showResourceForm.value) {
    resetResourceForm()
  }
}

const resetResourceForm = () => {
  resourceFormData.value = {
    name: '',
    description: '',
    sourceId: '',
    accessFormId: 1,
    discoveryServiceId: 1,
    contactEmail: '',
    uri: '',
  }
  resourceErrors.value = {}
}

const cancelResourceForm = () => {
  showResourceForm.value = false
  resetResourceForm()
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

const validateResourceForm = () => {
  resourceErrors.value = {}

  if (!resourceFormData.value.name?.trim()) {
    resourceErrors.value.name = 'Resource name is required'
  }

  if (!resourceFormData.value.description?.trim()) {
    resourceErrors.value.description = 'Resource description is required'
  }

  if (!resourceFormData.value.sourceId?.trim()) {
    resourceErrors.value.sourceId = 'Source ID is required'
  }

  if (!resourceFormData.value.accessFormId) {
    resourceErrors.value.accessFormId = 'Access Form ID is required'
  }

  if (!resourceFormData.value.discoveryServiceId) {
    resourceErrors.value.discoveryServiceId = 'Discovery Service ID is required'
  }

  if (resourceFormData.value.contactEmail && !isValidEmail(resourceFormData.value.contactEmail)) {
    resourceErrors.value.contactEmail = 'Please enter a valid email address'
  }

  if (resourceFormData.value.uri && !isValidUrl(resourceFormData.value.uri)) {
    resourceErrors.value.uri = 'Please enter a valid URL'
  }

  return Object.keys(resourceErrors.value).length === 0
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
  if (!validateForm()) return

  try {
    saving.value = true

    const updateData = {
      name: formData.value.name,
      externalId: formData.value.externalId,
      description: formData.value.description,
      contactEmail: formData.value.contactEmail || null,
      uri: formData.value.uri || null,
      withdrawn: formData.value.withdrawn,
    }

    const updatedOrganization = await organizationsStore.updateOrganization(
      props.organization.id,
      updateData,
    )
    emit('update', updatedOrganization)
  } catch (error) {
    console.error('Error updating organization:', error)
  } finally {
    saving.value = false
  }
}

const handleResourceSubmit = async () => {
  if (!validateResourceForm()) return

  try {
    savingResource.value = true

    const resourceData = {
      name: resourceFormData.value.name,
      description: resourceFormData.value.description,
      sourceId: resourceFormData.value.sourceId,
      organizationId: props.organization.id,
      accessFormId: resourceFormData.value.accessFormId,
      discoveryServiceId: resourceFormData.value.discoveryServiceId,
      contactEmail: resourceFormData.value.contactEmail || null,
      uri: resourceFormData.value.uri || null,
    }

    const createdResources = await resourcesStore.createResources([resourceData])
    emit('resourceAdded', createdResources)

    // Refresh the organization with resources
    await fetchOrganizationWithResources(props.organization.id)

    // Reset and hide the form
    cancelResourceForm()
  } catch (error) {
    console.error('Error creating resource:', error)
  } finally {
    savingResource.value = false
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
