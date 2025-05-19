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
    @confirm="hideFormSubmissionModal"
    requirement-link=""
  />
  <form-view-modal ref="formViewModalRef" id="formViewModal" :submittedForm="submittedForm?.payload" :isSubmittedFormSubmitted="submittedForm?.submitted" @editInfoSubmission="editInfoSubmission"/>
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

const props = defineProps({
  orgId: { type: String, default: undefined },
  org: { type: Object, default: () => ({}) },
  resourceStates: { type: Array, default: () => [] },
  negotiationId: { type: String, default: undefined },
})
const emit = defineEmits(['reloadResources'])

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
const isFormEditable = ref(false)

const openModal = async (href, resId) => {
  const requirement = await negotiationPageStore.retrieveInfoRequirement(href)
  resourceId.value = resId
  requiredAccessForm.value = requirement.requiredAccessForm
  requirementId.value = requirement.id
  formSubmissionModalInstance.value.show()
}

const openFormModal = async (href) => {
  const payload = await negotiationPageStore.retrieveInformationSubmission(href)
  submittedForm.value = payload
  formViewModalInstance.value.show()
}

const hideFormSubmissionModal = async () => {
  formSubmissionModalInstance.value.hide()
  emit('reloadResources')
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

function editInfoSubmission() {
  isFormEditable.value = true
  formViewModalInstance.value.hide()
  resourceId.value = submittedForm.value.resourceId
  //
  // this part needs to be changed after Radovan add form into retrieveInformationSubmission api response
  requiredAccessForm.value = {
    "id": 2,
    "name": "BBMRI.de Template",
    "sections": [
        {
            "id": 1,
            "name": "project",
            "label": "Project",
            "description": "Provide information about your project",
            "elements": [
                {
                    "id": 1,
                    "name": "title",
                    "label": "Title",
                    "description": "Give a title",
                    "type": "TEXT",
                    "required": true
                },
                {
                    "id": 2,
                    "name": "description",
                    "label": "Description",
                    "description": "Give a description",
                    "type": "TEXT_LARGE",
                    "required": true
                },
                {
                    "id": 6,
                    "name": "objective",
                    "label": "Study objective",
                    "description": "Study objective or hypothesis to be tested?",
                    "type": "TEXT",
                    "required": true
                },
                {
                    "id": 7,
                    "name": "profit",
                    "label": "Profit",
                    "description": "Is it a profit or a non-profit study",
                    "type": "BOOLEAN",
                    "required": true
                },
                {
                    "id": 8,
                    "name": "acknowledgment",
                    "label": "Acknowledgment",
                    "description": "Financing/ Acknowledgement or collaboration of the collection PIs?",
                    "type": "TEXT",
                    "required": true
                }
            ]
        },
        {
            "id": 2,
            "name": "request",
            "label": "Request",
            "description": "Provide information the resources you are requesting",
            "elements": [
                {
                    "id": 3,
                    "name": "description",
                    "label": "Description",
                    "description": "Provide a request description",
                    "type": "TEXT_LARGE",
                    "required": true
                },
                {
                    "id": 9,
                    "name": "diseaese-code",
                    "label": "Disease code",
                    "description": "What is the Disease being studied (ICD 10 code) ?",
                    "type": "TEXT",
                    "required": false
                },
                {
                    "id": 10,
                    "name": "collection",
                    "label": "Collection",
                    "description": "Is the collection to be prospective or retrospective?",
                    "type": "TEXT",
                    "required": false
                },
                {
                    "id": 11,
                    "name": "donors",
                    "label": "Donors",
                    "description": "How many different subjects (donors) would you need?",
                    "type": "NUMBER",
                    "required": true
                },
                {
                    "id": 12,
                    "name": "samples",
                    "label": "Samples",
                    "description": "What type(s) of samples and how many samples per subject are needed?",
                    "type": "TEXT",
                    "required": true
                },
                {
                    "id": 13,
                    "name": "specifics",
                    "label": "Specifics",
                    "description": "Are there any specific requirements ( e.g. volume,… )?",
                    "type": "TEXT",
                    "required": false
                }
            ]
        },
        {
            "id": 3,
            "name": "ethics-vote",
            "label": "Ethics vote",
            "description": "Is ethics vote present in your project?",
            "elements": [
                {
                    "id": 4,
                    "name": "ethics-vote",
                    "label": "Ethics vote",
                    "description": "Write the etchics vote",
                    "type": "TEXT_LARGE",
                    "required": true
                },
                {
                    "id": 5,
                    "name": "ethics-vote-attachment",
                    "label": "Attachment",
                    "description": "Upload Ethics Vote",
                    "type": "FILE",
                    "required": false
                }
            ]
        }
    ]
}
  requirementId.value = submittedForm.value.requirementId
  formSubmissionModalInstance.value.show()
}
</script>
