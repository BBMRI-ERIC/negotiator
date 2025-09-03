<template>
  <div class="resources-container">
    <!-- Resources Loading -->
    <div v-if="loading" class="resources-loading">
      <div class="spinner-border spinner-border-sm text-primary me-2" role="status">
        <span class="visually-hidden">Loading resources...</span>
      </div>
      <span>Loading resources...</span>
    </div>

    <!-- Resources List -->
    <div v-else-if="resources.length > 0" class="resources-list">
      <div class="resources-simple-list">
        <ResourceItem
          v-for="resource in resources"
          :key="resource.id"
          :resource="resource"
          @edit-resource="$emit('editResource', $event)"
          @representatives-updated="$emit('representativesUpdated', $event)"
        />
        <!-- Add Resources Button at bottom -->
        <div class="add-resources-container">
          <button
            class="btn btn-outline-primary add-resources-btn"
            @click="$emit('addResources')"
            title="Add more resources to this organization"
          >
            <i class="bi bi-plus-circle me-2"></i>
            Add Resources
          </button>
        </div>
      </div>
    </div>

    <!-- No Resources -->
    <div v-else class="no-resources">
      <div class="no-resources-content">
        <i class="bi bi-database text-muted"></i>
        <p class="text-muted mb-2">No resources found for this organization</p>
        <div class="add-resources-container">
          <button
            class="btn btn-outline-primary add-resources-btn"
            @click="$emit('addResources')"
            title="Add resources to this organization"
          >
            <i class="bi bi-plus-circle me-2"></i>
            Add Resources
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import ResourceItem from './ResourceItem.vue'

defineProps({
  resources: {
    type: Array,
    required: true,
  },
  loading: {
    type: Boolean,
    required: true,
  },
})

defineEmits(['editResource', 'addResources', 'representativesUpdated'])
</script>

<style scoped>
.resources-container {
  border-top: 1px solid #e9ecef;
  background: transparent;
  animation: slideDown 0.3s ease-out;
  margin-left: 3rem;
  margin-right: 1rem;
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 1000px;
  }
}

.resources-loading {
  padding: 1.5rem 2rem;
  text-align: center;
  color: #6c757d;
  display: flex;
  align-items: center;
  justify-content: center;
}

.resources-simple-list {
  padding: 0.75rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.no-resources {
  padding: 1.5rem;
  text-align: center;
}

.no-resources-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.no-resources-content i {
  font-size: 2rem;
  color: #dee2e6;
}

.add-resources-container {
  display: flex;
  justify-content: flex-end;
  padding: 1rem 0;
}

.add-resources-btn {
  padding: 0.5rem 1.5rem;
  font-size: 0.95rem;
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
}

.add-resources-btn i {
  font-size: 1.2rem;
  margin-right: 0.5rem;
}
</style>
