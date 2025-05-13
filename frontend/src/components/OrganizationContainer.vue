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

  <!-- Organization Card Component -->
  <OrganizationCard
    :org-id="orgId"
    :org="org"
    :resource-states="resourceStates"
    :negotiation-id="negotiationId"
    :ui-configuration="uiConfiguration"
    @open-form-modal="openFormModal"
    @open-modal="openModal"
    @update-resource-state="updateResourceState"
    @update-org-status="updateOrgStatus"
  />
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import Modal from 'bootstrap/js/dist/modal'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import OrganizationCard from './OrganizationCard.vue'
import FormViewModal from '@/components/modals/FormViewModal.vue'
import FormSubmissionModal from '@/components/modals/FormSubmissionModal.vue'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  orgId: { type: String, default: undefined },
  org: { type: Object, default: () => ({}) },
  resourceStates: { type: Array, default: () => [] },
  negotiationId: { type: String, default: undefined }
})
const emit = defineEmits(['reloadResources'])

const router = useRouter()
const uiConfigurationStore = useUiConfiguration()
const negotiationPageStore = useNegotiationPageStore()
const uiConfiguration = computed(() => uiConfigurationStore.uiConfiguration?.theme)

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

const openModal = async (href, resId) => {
  const requirement = await negotiationPageStore.retrieveInfoRequirement(href)
  resourceId.value = resId
  requiredAccessForm.value = requirement.requiredAccessForm
  requirementId.value = requirement.id
  formSubmissionModalInstance.value.show()
}

const openFormModal = async (href) => {
  const payload = await negotiationPageStore.retrieveInformationSubmission(href)
  submittedForm.value = payload.payload
  formViewModalInstance.value.show()
}

const hideFormSubmissionModal = async () => {
  formSubmissionModalInstance.value.hide()
  emit('reloadResources')
  router.go()
}

const updateResourceState = async (link) => {
  await negotiationPageStore.updateResourceStatus(link)
  emit('reloadResources')
}

const updateOrgStatus = (state, organization) => {
  selectedOrganization.value = organization
  orgStatus.value = state
}

// Update organization with new status for all represented resources
const getRepresentedResources = (resources) =>
  resources.filter(resource =>
    Object.values(resource._links).some(link => link.title === 'Next Lifecycle event')
  ).map(resource => resource.id)

const updateOrganization = async () => {
  const data = {
    resourceIds: getRepresentedResources(selectedOrganization.value.resources),
    state: orgStatus.value.value,
  }
  await negotiationPageStore.addResources(data, props.negotiationId)
  emit('reloadResources')
}

onMounted(() => {
  formSubmissionModalInstance.value = new Modal(formSubmissionModalRef.value.$el)
  formViewModalInstance.value = new Modal(formViewModalRef.value.$el)
})

onUnmounted(() => {
  // Cleanup if needed
})
</script>
