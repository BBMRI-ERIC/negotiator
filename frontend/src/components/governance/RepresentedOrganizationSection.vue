<template>
  <div class="organizations-section">
    <OrganizationsSectionHeader
      :loading="organizationsLoading"
      :all-expanded="allExpanded"
    />

    <OrganizationsFilters
      :filters="filters"
      :loading="organizationsLoading"
      @update-filters="updateFilters"
      @clear-filters="clearFilters"
      @debounced-search="debouncedSearch"
      @apply-filters="applyFilters"
    />

    <OrganizationsList
      :organizations="organizationsList"
      :loading="organizationsLoading"
      :expanded-organizations="expandedOrganizations"
      :organization-resources="organizationResources"
      :loading-resources="loadingResources"
      :no-results-message="getNoResultsMessage()"
      :sorted-resources-for-organization="sortedResourcesForOrganization"
      @toggle-organization="toggleOrganization"
    />
  </div>
</template>

<script setup>
import { useResources } from '@/composables/useResources'

import OrganizationsSectionHeader from './OrganizationsSectionHeader.vue'
import OrganizationsFilters from './OrganizationsFilters.vue'
import OrganizationsList from './OrganizationsList.vue'

const resources = useResources()

const {
  organizations: organizationsList,
  organizationResources,
  loadingResources,
  expandedOrganizations,
  loading: organizationsLoading,
  filters,
  allExpanded,
  getNoResultsMessage,
  sortedResourcesForOrganization,
  toggleOrganization,
  updateFilters,
  clearFilters,
  applyFilters,
  debouncedSearch,
} = resources

</script>

<style scoped>
.organizations-section {
  background: #ffffff;
  border-radius: 0.5rem;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
}

@media (max-width: 768px) {
  .organizations-section {
    padding: 1rem;
  }
}
</style>
