<template>
  <div class="specific-area panel panel-default border-">
    <div class="d-flex justify-content-between align-items-center mb-1">
      <h2 class="text-left">Information Requirements</h2>
      <button class="btn btn-success" @click="addRequirement">Add Requirement</button>
    </div>
    <div class="text-muted mb-3">
      Information requirements ensure Representatives of Resources in a Negotiation provide
      additional information before advancing to a chosen state. Learn more in our
      <a
        href="https://bbmri-eric.github.io/negotiator/administrator#additional-information-requirements-guide"
        target="_blank"
        rel="noopener noreferrer"
        >Information Requirements Documentation</a
      >.
    </div>

    <!-- The modal component -->
    <InformationRequirementModal
      :show="showModal"
      :accessForms="accessForms"
      :resourceAllEvents="resourceAllEvents"
      @set-info-requirements="setInfoRequirements"
      @close-modal="closeModal"
    />

    <div v-if="!infoRequirements?.['info-requirements']?.length" class="alert alert-light my-3">
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
import { computed, ref } from 'vue'
import InformationRequirementModal from '../components/modals/InfoRequirementModal.vue'

const props = defineProps({
  resourceAllEvents: {
    type: Object,
    required: true,
  },
  infoRequirements: {
    type: Object,
    required: true,
  },
  accessForms: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['set-info-requirements'])

const showModal = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

const paginatedRequirements = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return props.infoRequirements?.['info-requirements']?.slice(start, end) || []
})

function getEventLabel(eventValue) {
  const event = props.resourceAllEvents.find((e) => e.value === eventValue)
  return event ? event.label : 'Unknown'
}

function setInfoRequirements(data) {
  try {
    // Emit the data to the parent component to handle store update
    emit('set-info-requirements', data)
    // Close the modal after successful emission
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
