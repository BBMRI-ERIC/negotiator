<template>
  <!-- Loading State -->
  <div v-if="loading && organizations.length === 0" class="loading-container">
    <div class="loading-spinner">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading organizations...</span>
      </div>
      <p class="loading-text">Loading organizations and resources...</p>
    </div>
  </div>

  <!-- No Results -->
  <div v-else-if="organizations.length === 0 && !loading" class="no-results">
    <div class="no-results-icon">
      <i class="bi bi-building"></i>
    </div>
    <h4>No Organizations Found</h4>
    <p class="text-muted">{{ noResultsMessage }}</p>
  </div>

  <!-- Organizations List -->
  <div v-else class="organizations-list">
    <OrganizationCard
      v-for="organization in organizations"
      :key="organization.id"
      :organization="organization"
      :is-expanded="expandedOrganizations.has(organization.id)"
      :resources="sortedResourcesForOrganization(organization.id)"
      :resources-loading="loadingResources.has(organization.id)"
      :is-admin="isAdmin"
      @toggle-expanded="$emit('toggleOrganization', organization.id)"
      @edit-organization="$emit('editOrganization', organization)"
      @edit-resource="$emit('editResource', $event)"
      @add-resources="$emit('addResources', organization.id)"
      @representatives-updated="$emit('representativesUpdated', $event)"
    />
  </div>
</template>

<script setup>
import OrganizationCard from './OrganizationCard.vue'

defineProps({
  isAdmin: {
    type: Boolean,
    default: false,
  },
  organizations: {
    type: Array,
    required: true,
  },
  loading: {
    type: Boolean,
    required: true,
  },
  expandedOrganizations: {
    type: Set,
    required: true,
  },
  organizationResources: {
    type: Object,
    required: true,
  },
  loadingResources: {
    type: Set,
    required: true,
  },
  noResultsMessage: {
    type: String,
    required: true,
  },
  sortedResourcesForOrganization: {
    type: Function,
    required: true,
  },
})

defineEmits([
  'toggleOrganization',
  'editOrganization',
  'editResource',
  'addResources',
  'representativesUpdated',
])
</script>

<style scoped>
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 4rem 2rem;
}

.loading-spinner {
  text-align: center;
}

.loading-text {
  margin-top: 1rem;
  color: #6c757d;
  font-size: 1rem;
}

.no-results {
  text-align: center;
  padding: 4rem 2rem;
  color: #6c757d;
}

.no-results-icon i {
  font-size: 4rem;
  color: #dee2e6;
  margin-bottom: 1.5rem;
}

.organizations-list {
  margin-bottom: 2rem;
}
</style>
