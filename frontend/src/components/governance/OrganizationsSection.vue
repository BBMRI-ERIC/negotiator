<template>
  <div class="organizations-section">
    <OrganizationsSectionHeader
      :loading="organizationsLoading || operationsLoading"
      :all-expanded="allExpanded"
      :is-admin="props.isAdmin"
      @create-organization="modals.openCreateModal"
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
      :is-admin="props.isAdmin"
      @toggle-organization="toggleOrganization"
      @edit-organization="modals.openEditModal"
      @edit-resource="modals.openEditResourceModal"
      @add-resources="handleAddResources"
      @representatives-updated="handleRepresentativesUpdated"
    />

    <OrganizationsPagination
      :page-number="pageNumber"
      :total-pages="totalPages"
      :total-elements="totalElements"
      :page-size="pageSize"
      :loading="organizationsLoading"
      @previous-page="previousPage"
      @next-page="nextPage"
      @update-page-size="updatePageSize"
      v-if="props.isAdmin"
    />

    <EditOrganizationModal
      modal-id="editOrganizationModal"
      :organization="modals.selectedOrganization.value"
      :shown="modals.showEditModal.value"
      @update="handleOrganizationUpdate"
      @close="modals.closeEditModal"
      v-if="props.isAdmin"
    />

    <CreateOrganizationModal
      modal-id="createOrganizationModal"
      :shown="modals.showCreateModal.value"
      @create="handleOrganizationCreate"
      @close="modals.closeCreateModal"
      v-if="props.isAdmin"
    />

    <CreateResourceModal
      modal-id="createResourceModal"
      :organization-id="modals.selectedOrganizationForResource.value?.id"
      :organization-name="modals.selectedOrganizationForResource.value?.name"
      :shown="modals.showCreateResourceModal.value"
      @create="handleResourceCreate"
      @close="modals.closeCreateResourceModal"
      v-if="props.isAdmin"
    />

    <EditResourceModal
      modal-id="editResourceModal"
      :resource="modals.selectedResource.value"
      :shown="modals.showEditResourceModal.value"
      @update="handleResourceUpdate"
      @close="modals.closeEditResourceModal"
      v-if="props.isAdmin"
    />
  </div>
</template>

<script setup>
import { useOrganizations } from '@/composables/useOrganizations'
import { useOrganizationModals } from '@/composables/useOrganizationModals'
import { useOrganizationOperations } from '@/composables/useOrganizationOperations'

import OrganizationsSectionHeader from './OrganizationsSectionHeader.vue'
import OrganizationsFilters from './OrganizationsFilters.vue'
import OrganizationsList from './OrganizationsList.vue'
import OrganizationsPagination from './OrganizationsPagination.vue'
import CreateOrganizationModal from './CreateOrganizationModal.vue'
import EditOrganizationModal from './EditOrganizationModal.vue'
import CreateResourceModal from './CreateResourceModal.vue'
import EditResourceModal from './EditResourceModal.vue'

const organizations = useOrganizations()
const modals = useOrganizationModals()
const operations = useOrganizationOperations()

const props = defineProps({
  isAdmin: {
    type: Boolean,
    default: false,
  },
})

const {
  organizations: organizationsList,
  organizationResources,
  loadingResources,
  expandedOrganizations,
  loading: organizationsLoading,
  pageNumber,
  totalPages,
  totalElements,
  pageSize,
  filters,
  allExpanded,
  getNoResultsMessage,
  sortedResourcesForOrganization,
  toggleOrganization,
  loadOrganizations,
  previousPage,
  nextPage,
  updatePageSize,
  updateFilters,
  clearFilters,
  applyFilters,
  debouncedSearch,
  reloadResourcesForOrganization,
} = organizations

const {
  loading: operationsLoading,
  handleOrganizationCreate: baseHandleOrganizationCreate,
  handleOrganizationUpdate: baseHandleOrganizationUpdate,
  handleResourceCreate: baseHandleResourceCreate,
  handleResourceUpdate: baseHandleResourceUpdate,
} = operations

const handleAddResources = (organizationId) => {
  const organization = organizationsList.value.find((org) => org.id === organizationId)
  if (organization) {
    modals.openCreateResourceModal(organization)
  }
}

const handleOrganizationCreate = (newOrganization) => {
  baseHandleOrganizationCreate(newOrganization, {
    loadOrganizations,
    closeCreateModal: modals.closeCreateModal,
  })
}

const handleOrganizationUpdate = (updateData) => {
  baseHandleOrganizationUpdate(updateData, {
    loadOrganizations,
    closeEditModal: modals.closeEditModal,
  })
}

const handleResourceCreate = (createdResourceData) => {
  baseHandleResourceCreate(createdResourceData, {
    selectedOrganizationForResource: modals.selectedOrganizationForResource,
    reloadResourcesForOrganization,
    closeCreateResourceModal: modals.closeCreateResourceModal,
  })
}

const handleResourceUpdate = (updatedResourceData) => {
  baseHandleResourceUpdate(updatedResourceData, {
    selectedResource: modals.selectedResource,
    organizationResources,
    reloadResourcesForOrganization,
    closeEditResourceModal: modals.closeEditResourceModal,
  })
}

const handleRepresentativesUpdated = (eventData) => {
  const { resourceId } = eventData
  if (resourceId) {
    const resource = Object.values(organizationResources.value)
      .flat()
      .find((r) => r.id === resourceId)

    if (resource) {
      const organizationId = organizationsList.value.find((org) =>
        organizationResources.value[org.id]?.some((r) => r.id === resourceId),
      )?.id

      if (organizationId) {
        reloadResourcesForOrganization(organizationId)
      }
    }
  }
}
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
