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
    :isFormEditable="isFormEditable"
    v-model:submittedForm="submittedForm"
    @confirm="hideFormSubmissionModal"
    @confirmUpdate="hideFormSubmissionAndOpenView"
    requirement-link=""
  />
  <form-view-modal
    ref="formViewModalRef"
    id="formViewModal"
    :submittedForm="submittedForm"
    :isAdmin="isAdmin"
    @editInfoSubmission="editInfoSubmission"
  />
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
    @editInfoSubmission="editInfoSubmission"
    @update-org-status="updateOrgStatus"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import Modal from 'bootstrap/js/dist/modal'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import { useFormsStore } from '../store/forms'
import OrganizationCard from './OrganizationCard.vue'
import FormViewModal from '@/components/modals/FormViewModal.vue'
import FormSubmissionModal from '@/components/modals/FormSubmissionModal.vue'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'

const props = defineProps({
  orgId: { type: String, default: undefined },
  org: { type: Object, default: () => ({}) },
  resourceStates: { type: Array, default: () => [] },
  negotiationId: { type: String, default: undefined },
  isAdmin: { type: Boolean, default: false },
})
const emit = defineEmits(['reloadResources'])

const uiConfigurationStore = useUiConfiguration()
const negotiationPageStore = useNegotiationPageStore()
const formsStore = useFormsStore()
const uiConfiguration = computed(() => uiConfigurationStore.uiConfiguration?.theme)

// Modal and Form Data
const requirementId = ref(undefined)
const resourceId = ref(undefined)
const requiredAccessForm = ref({})
const submittedForm = ref({})
const selectedOrganization = ref(undefined)
const orgStatus = ref(undefined)

// Modal Refs and Instances
const formSubmissionModalRef = ref(null)
const formSubmissionModalInstance = ref(null)
const formViewModalRef = ref(null)
const formViewModalInstance = ref(null)
const isFormEditable = ref(false)

const openModal = async (href, resId) => {
  isFormEditable.value = false
  const requirement = await negotiationPageStore.retrieveInfoRequirement(href)
  resourceId.value = resId
  requiredAccessForm.value = requirement.requiredAccessForm
  requirementId.value = requirement.id
  formSubmissionModalInstance.value.show()
}

async function openFormModal(href) {
  await negotiationPageStore.retrieveInformationSubmission(href).then((res) => {
    submittedForm.value = res
    formViewModalInstance.value.show()
  })
}

function hideFormSubmissionModal() {
  formSubmissionModalInstance.value.hide()
  emit('reloadResources')
}

function hideFormSubmissionAndOpenView(payload) {
  submittedForm.value.payload = payload
  formSubmissionModalInstance.value.hide()
  emit('reloadResources')
  formViewModalInstance.value.show()
}

async function updateResourceState(link) {
  await negotiationPageStore.updateResourceStatus(link)
  emit('reloadResources')
}

const updateOrgStatus = (state, organization) => {
  selectedOrganization.value = organization
  orgStatus.value = state
}

// Update organization with new status for all represented resources
const getRepresentedResources = (resources) =>
  resources
    .filter((resource) =>
      Object.values(resource._links).some((link) => link.title === 'Next Lifecycle event'),
    )
    .map((resource) => resource.id)

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

async function editInfoSubmission(href) {
  if (href) {
    await negotiationPageStore.retrieveInformationSubmission(href).then((res) => {
      submittedForm.value = res
      formViewModalInstance.value.show()
    })
  }

  isFormEditable.value = true
  formViewModalInstance.value.hide()
  resourceId.value = submittedForm.value.resourceId
  requirementId.value = submittedForm.value.requirementId
  formsStore.retrieveInfoRequirementsById(submittedForm.value.requirementId).then((res) => {
    requiredAccessForm.value = res.requiredAccessForm
  })
  formSubmissionModalInstance.value.show()
}
</script>
