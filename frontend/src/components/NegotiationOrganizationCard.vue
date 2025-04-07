<template>
  <!-- Modals -->
  <form-submission-modal
    ref="formSubmissionModalRef"
    id="formSubmissionModal"
    :title="requiredAccessForm.name"
    :negotiation-id="negotiationId"
    :requirement-id="requirementId"
    :resource-id="resourceId"
    :required-access-form-id="requiredAccessForm.id"
    @confirm="hideFormSubmissionModal"
    requirement-link=""
  />
  <form-view-modal ref="formViewModalRef" id="formViewModal" :payload="submittedForm" />
  <confirmation-modal
    id="statusUpdateModal"
    :title="`Status update for ${selectedOrganization ? selectedOrganization.name : 'Unknown'}`"
    :text="`Are you sure you want to change the status of all ${selectedOrganization ? selectedOrganization.name : 'Unknown'} resources you represent in this Negotiation to ${orgStatus ? orgStatus.label : 'Unknown'}?`"
    :message-enabled="false"
    @confirm="updateOrganization"
  />

  <!-- Organization Card -->
  <div class="card mb-2">
    <div class="card-header">
      <div class="form-check d-flex align-items-center">
        <!-- Collapse Organization Header -->
        <div
          class="collapse-organization d-flex align-items-center cursor-pointer"
          data-bs-toggle="collapse"
          aria-expanded="false"
          :data-bs-target="`#card-body-block-${sanitizeId(orgId)}`"
          :aria-controls="`card-body-block-${sanitizeId(orgId)}`"
          @click="toggleCollapse(orgId)"
        >
          <i class="bi" :class="isCollapsed[orgId] ? 'bi-chevron-right' : 'bi-chevron-down'" />
          <i class="bi bi-buildings mx-2" :style="{ color: uiConfiguration.primaryTextColor }" />
          <span class="fw-bold" :style="{ color: uiConfiguration.secondaryTextColor }">
            {{ org.name }}
          </span>
        </div>

        <!-- Status Dropdown -->
        <div class="status-dropdown-container ms-auto" :data-org-id="orgId">
          <button
            type="button"
            class="status-box p-1 d-flex align-items-center btn"
            title="Select current status. The term Resource is abstract and can for example refer to biological samples, datasets or a service such as sequencing."
            @click.stop="toggleDropdown(orgId)"
          >
            <span class="badge" :class="getStatusColor(org.status)">
              <i :class="getStatusIcon(org.status)" class="px-1" />
              {{ org.status?.replace(/_/g, ' ') || '' }}
            </span>
            <i
              v-if="org.updatable"
              class="bi icon-smaller mx-1"
              :class="dropdownVisible[orgId] ? 'bi-caret-up-fill' : 'bi-caret-down-fill'"
            />
          </button>
          <ul v-if="org.updatable && dropdownVisible[orgId]" class="dropdown-menu show">
            <li
              v-for="state in sortedStates"
              :key="state.value"
              class="dropdown-item cursor-pointer"
              data-bs-toggle="modal"
              data-bs-target="#statusUpdateModal"
              @click="updateOrgStatus(state, org, orgId)"
            >
              <i :class="getStatusIcon(state.value)" class="px-1" />
              {{ state.label }}
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- Organization Resources -->
    <div :id="`card-body-block-${sanitizeId(orgId)}`" class="collapse multi-collapse">
      <div v-for="resource in org.resources" :key="resource.id" class="card-body">
        <div class="form-check d-flex flex-column">
          <div class="d-flex flex-row align-items-center flex-wrap">
            <div class="me-3 mb-2">
              <label
                class="form-check-label text-truncate"
                :style="{ color: uiConfiguration.primaryTextColor, maxWidth: '300px' }"
                :title="resource.name"
                :for="sanitizeId(resource.sourceId)"
              >
                <i class="bi bi-box-seam" />
                {{ resource.name }}
              </label>
              <span class="badge rounded-pill bg-status-badge ms-2">
                {{ getStatusForResource(resource.id) }}
              </span>
            </div>

            <!-- Resource URI if available -->
            <div v-if="resource.uri" class="me-3 mb-2">
              <a :href="resource.uri" target="_blank" class="text-decoration-none">
                <i class="bi bi-box-arrow-up-right" /> View Details
              </a>
            </div>

            <div class="d-flex flex-wrap">
              <div class="me-3 mb-2">
                <span :style="{ color: uiConfiguration.primaryTextColor, opacity: 0.7 }">
                  {{ resource.sourceId }}
                  <CopyTextButton :text="resource.sourceId" />
                </span>
              </div>

              <!-- Resource Lifecycle Links -->
              <div v-if="getLifecycleLinks(resource._links).length" class="me-3 mb-2">
                Update status:
                <div
                  v-for="(link, index) in getLifecycleLinks(resource._links)"
                  :key="index"
                  class="lifecycle-links d-inline-block ms-2"
                >
                  <a class="lifecycle-text cursor-pointer" @click="updateResourceState(link.href)">
                    <i class="bi bi-patch-check" /> {{ link.name }}
                  </a>
                </div>
              </div>
            </div>
          </div>

          <!-- Submitted Requirements (Green) -->
          <div
            v-for="(link, index) in getSubmissionLinks(resource._links)"
            :key="index"
            class="mt-1"
          >
            <a class="submission-text cursor-pointer" @click.prevent="openFormModal(link.href)">
              <i class="bi bi-check-circle" /> {{ link.name }}
            </a>
          </div>

          <!-- Missing Requirements (Red) -->
          <div
            v-for="(link, index) in getRequirementLinks(resource._links)"
            :key="index"
            class="mt-1"
          >
            <a class="requirement-text cursor-pointer" @click="openModal(link.href, resource.id)">
              <i class="bi bi-exclamation-circle-fill" /> {{ link.title }} required
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import Modal from 'bootstrap/js/dist/modal'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import { getStatusColor, getStatusIcon, transformStatus } from '../composables/utils.js'
import CopyTextButton from '@/components/CopyTextButton.vue'
import FormViewModal from '@/components/modals/FormViewModal.vue'
import FormSubmissionModal from '@/components/modals/FormSubmissionModal.vue'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'

const props = defineProps({
  orgId: { type: String, default: undefined },
  org: { type: Object, default: () => ({}) },
  resources: { type: Array, default: () => [] },
  resourceStates: { type: Array, default: () => [] },
  negotiationId: { type: String, default: undefined }
})

const emit = defineEmits(['reloadResources'])

const uiConfigurationStore = useUiConfiguration()
const negotiationPageStore = useNegotiationPageStore()

const uiConfiguration = computed(() => uiConfigurationStore.uiConfiguration?.theme)
const getResources = computed(() => props.resources)
const resourcesById = computed(() =>
  getResources.value.reduce((acc, resource) => {
    acc[resource.id] = resource
    return acc
  }, {})
)
const sortedStates = computed(() =>
  props.resourceStates.slice().sort((a, b) => Number(a.ordinal) - Number(b.ordinal))
)

// UI State
const dropdownVisible = reactive({})
const isCollapsed = reactive({})

// Modal and Form Data
const requirementId = ref(undefined)
const resourceId = ref(undefined)
const requiredAccessForm = ref({})
const submittedForm = ref(undefined)
const selectedOrganization = ref(undefined)
const orgStatus = ref(undefined)

// Modal Refs and Instances
const formSubmissionModalRef = ref(null)
const formSubmissionModalInstance = ref(null)
const formViewModalRef = ref(null)
const formViewModalInstance = ref(null)

// Utility: Replace disallowed characters in an ID string
const sanitizeId = (id) => id.replaceAll(':', '_')

// Toggle the status dropdown visibility for an organization
const toggleDropdown = (orgId) => {
  Object.keys(dropdownVisible).forEach(key => {
    if (key !== orgId) dropdownVisible[key] = false
  })
  dropdownVisible[orgId] = !dropdownVisible[orgId]
}

// Toggle collapse for an organization
const toggleCollapse = (orgId) => {
  isCollapsed[orgId] = !isCollapsed[orgId]
}

// Update organization status and store selected org/state for later processing
const updateOrgStatus = (state, organization, orgId) => {
  toggleDropdown(orgId)
  selectedOrganization.value = organization
  orgStatus.value = state
}

// Get a resource’s current status label (if available)
const getStatusForResource = (resourceId) => {
  const currentState = resourcesById.value[resourceId]?.currentState
  return currentState ? transformStatus(currentState) : ''
}

// Update the resource state and then trigger a reload
const updateResourceState = async (link) => {
  await negotiationPageStore.updateResourceStatus(link)
  emit('reloadResources')
}

// Filter links by key prefix (submission or requirement)
const getSubmissionLinks = (links) =>
  Object.entries(links)
    .filter(([key]) => key.startsWith('submission-'))
    .map(([, value]) => value)

const getRequirementLinks = (links) =>
  Object.entries(links)
    .filter(([key]) => key.startsWith('requirement-'))
    .map(([, value]) => value)

// Get lifecycle links where the title matches the expected event
const getLifecycleLinks = (links) =>
  Object.values(links).filter(link => link.title === 'Next Lifecycle event')

// Open the form submission modal and load requirement info
const openModal = async (href, resId) => {
  const requirement = await negotiationPageStore.retrieveInfoRequirement(href)
  resourceId.value = resId
  requiredAccessForm.value = requirement.requiredAccessForm
  requirementId.value = requirement.id
  formSubmissionModalInstance.value.show()
}

// Open the form view modal and load submitted form data
const openFormModal = async (href) => {
  const payload = await negotiationPageStore.retrieveInformationSubmission(href)
  submittedForm.value = payload.payload
  formViewModalInstance.value.show()
}

// Hide the form submission modal and trigger a resource reload
const hideFormSubmissionModal = async () => {
  formSubmissionModalInstance.value.hide()
  emit('reloadResources')
}

// Get IDs of resources represented by an organization (i.e. those having a next lifecycle event)
const getRepresentedResources = (resources) =>
  resources
    .filter(resource =>
      Object.values(resource._links).some(link => link.title === 'Next Lifecycle event')
    )
    .map(resource => resource.id)

// Update organization with new status for all represented resources
const updateOrganization = async () => {
  const data = {
    resourceIds: getRepresentedResources(selectedOrganization.value.resources),
    state: orgStatus.value.value,
  }
  await negotiationPageStore.addResources(data, props.negotiationId)
  emit('reloadResources')
}

// Close dropdown when clicking outside
const handleClickOutside = (event) => {
  Object.keys(dropdownVisible).forEach(orgId => {
    const dropdownElement = document.querySelector(`.status-dropdown-container[data-org-id="${orgId}"]`)
    if (dropdownVisible[orgId] && dropdownElement && !dropdownElement.contains(event.target)) {
      dropdownVisible[orgId] = false
    }
  })
}

onMounted(() => {
  // Initialize modal instances using component refs
  formSubmissionModalInstance.value = new Modal(formSubmissionModalRef.value.$el)
  formViewModalInstance.value = new Modal(formViewModalRef.value.$el)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* Collapse Icon */
.collapse-organization {
  transition: all 0.2s ease;
}

/* Status Dropdown Container */
.status-dropdown-container {
  position: relative;
  display: inline-block;
}

/* Desktop dropdown menu */
.status-dropdown-container .dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 0.5rem;
  z-index: 1000;
}

/* Mobile adjustments */
@media (max-width: 576px) {
  .status-dropdown-container {
    width: 100%;
    text-align: center;
    margin-top: 0.5rem;
  }

  .status-dropdown-container .dropdown-menu {
    position: relative;
    top: 0;
    left: 0;
    width: 100%;
    margin-top: 0.5rem;
    text-align: left;
  }
}

/* Requirement (missing) - red text */
.requirement-text {
  color: red;
  opacity: 0.8;
}

/* Submission (submitted) - green text */
.submission-text {
  color: green;
  font-weight: bold;
}

/* Text truncation */
.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  vertical-align: middle;
}

/* Resource link */
.resource-link {
  color: var(--bs-link-color);
  text-decoration: none;
}

.resource-link:hover {
  text-decoration: underline;
}

/* Badge styling */
.bg-status-badge {
  background-color: #f0f0f0;
  color: #333;
}

/* Status box */
.status-box {
  display: flex;
  align-items: center;
}

/* Icon size */
.icon-smaller {
  font-size: 0.8rem;
}
</style>
