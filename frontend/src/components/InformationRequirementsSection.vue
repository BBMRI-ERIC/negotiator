<template>
  <div class="specific-area panel panel-default border-">
    <AdminSettingsPageHeader title="Information Requirements">
      <template #actions>
        <button class="btn btn-sm btn-outline-primary" @click="addRequirement">
          Add Requirement
        </button>
      </template>
      <template #description>
        Information requirements ensure Representatives of Resources in a Negotiation provide
        additional information before advancing to a chosen state. Learn more in our
        <a
          href="https://bbmri-eric.github.io/negotiator/administrator#additional-information-requirements-guide"
          target="_blank"
          rel="noopener noreferrer"
          >Information Requirements Documentation</a
        >.
      </template>
    </AdminSettingsPageHeader>

    <!-- The modal component -->
    <InformationRequirementModal
      :show="showModal"
      :accessForms="accessForms"
      :resourceAllEvents="resourceAllEvents"
      @set-info-requirements="setInfoRequirements"
      @close-modal="closeModal"
    />

    <div v-if="isLoading" class="text-center py-4">
      <LoadingIndicator />
    </div>

    <div
      v-else-if="!infoRequirements?.['info-requirements']?.length"
      class="alert alert-light my-3"
    >
      No information requirements configured.
    </div>

    <div v-else class="table-container">
      <table class="table table-hover">
        <thead>
          <tr>
            <th scope="col">ID</th>
            <th scope="col">Access Form</th>
            <th scope="col">Lifecycle Event</th>
            <th scope="col">Admin Only</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="requirement in paginatedRequirements" :key="requirement.id">
            <td>{{ requirement.id }}</td>
            <td>{{ requirement.requiredAccessForm.name }}</td>
            <td>{{ getEventLabel(requirement.forResourceEvent) }}</td>
            <td>{{ requirement.viewableOnlyByAdmin ? 'Yes' : 'No' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import InformationRequirementModal from '../components/modals/InfoRequirementModal.vue'
import AdminSettingsPageHeader from '@/components/AdminSettingsPageHeader.vue'
import LoadingIndicator from '@/components/LoadingIndicator.vue'
import { useAdminStore } from '@/store/admin.js'
import { useFormsStore } from '@/store/forms.js'

const adminStore = useAdminStore()
const formsStore = useFormsStore()

const resourceAllEvents = ref([])
const infoRequirements = ref({})
const accessForms = ref([])
const isLoading = ref(false)

const showModal = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

onMounted(async () => {
  isLoading.value = true
  try {
    ;[resourceAllEvents.value, infoRequirements.value, accessForms.value] = await Promise.all([
      adminStore.retrieveResourceAllEvents(),
      adminStore.retrieveInfoRequirements(),
      formsStore.retrieveAllAccessForms(),
    ])
  } finally {
    isLoading.value = false
  }
})

const paginatedRequirements = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return infoRequirements.value?.['info-requirements']?.slice(start, end) || []
})

function getEventLabel(eventValue) {
  const event = resourceAllEvents.value.find((e) => e.value === eventValue)
  return event ? event.label : 'Unknown'
}

async function setInfoRequirements(data) {
  try {
    await adminStore.setInfoRequirements(data)
    infoRequirements.value = await adminStore.retrieveInfoRequirements()
    closeModal()
  } catch (error) {
    console.error('Error setting info requirements:', error)
  }
}

function addRequirement() {
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}
</script>

<style scoped>
.specific-area {
  position: relative;
}

.container {
  padding: 0;
}

.text-muted {
  font-size: 0.95rem;
  color: #6c757d;
}

.table-container {
  margin-top: 1rem;
}
</style>
