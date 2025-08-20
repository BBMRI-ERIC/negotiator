<template>
  <div
    class="resource-item"
    :class="{ 'withdrawn-resource': resource.withdrawn || resource.status === 'withdrawn' }"
  >
    <div class="resource-content">
      <span class="resource-name">{{ resource.name }}</span>
      <span class="resource-id">{{ resource.sourceId || resource.id }}</span>
    </div>
    <div class="resource-actions">
      <button
        class="btn btn-sm btn-outline-primary me-2"
        data-bs-toggle="modal"
        :data-bs-target="`#resourceRepresentativesModal-${resource.id}`"
        title="Manage Representatives"
        @click="prepareModal"
      >
        <i class="bi bi-people"></i>
      </button>
      <button
        class="btn btn-sm btn-outline-secondary"
        @click="$emit('editResource', resource)"
        title="Edit Resource"
      >
        <i class="bi bi-pencil"></i>
      </button>
    </div>
  </div>

  <!-- Resource Representatives Modal -->
  <ResourceRepresentativesModal
    :modal-id="`resourceRepresentativesModal-${resource.id}`"
    :resource="selectedResource"
    @representatives-updated="handleRepresentativesUpdated"
  />
</template>

<script setup>
import { ref } from 'vue'
import ResourceRepresentativesModal from '../modals/ResourceRepresentativesModal.vue'

const props = defineProps({
  resource: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['editResource', 'representativesUpdated'])

const selectedResource = ref(null)

const prepareModal = () => {
  selectedResource.value = props.resource
}

const handleRepresentativesUpdated = (data) => {
  emit('representativesUpdated', data)
}
</script>

<style scoped>
.resource-item {
  background: #f0f8ff;
  border: 2px solid #b3d9ff;
  border-radius: 0.375rem;
  padding: 0.75rem;
  transition: all 0.2s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-left: 0.5rem;
}

.resource-item:hover {
  background: #e6f3ff;
  border-color: #0d6efd;
  box-shadow: 0 2px 8px rgba(13, 110, 253, 0.15);
}

.resource-item.withdrawn-resource {
  background: #f8f9fa;
  border-color: #d1d1d1;
  opacity: 0.7;
}

.resource-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.resource-name {
  font-weight: 500;
  color: #2c3e50;
}

.resource-id {
  font-size: 0.875rem;
  color: #6c757d;
}
</style>
