import { ref } from 'vue'
import { useAdminStore } from '@/store/admin'

export function useOrganizationOperations() {
  const adminStore = useAdminStore()
  const loading = ref(false)

  const handleOrganizationCreate = async (
    newOrganization,
    { loadOrganizations, closeCreateModal },
  ) => {
    try {
      loading.value = true
      await adminStore.createOrganization(newOrganization)
      await loadOrganizations()
      closeCreateModal()
    } catch (error) {
      console.error('Error creating organization:', error)
    } finally {
      loading.value = false
    }
  }

  const handleOrganizationUpdate = async (
    { organizationId, updateData },
    { loadOrganizations, closeEditModal },
  ) => {
    try {
      loading.value = true
      await adminStore.updateOrganization(organizationId, updateData)
      await loadOrganizations()
      closeEditModal()
    } catch (error) {
      console.error('Error updating organization:', error)
    } finally {
      loading.value = false
    }
  }

  const handleResourceCreate = async (
    createdResourceData,
    { selectedOrganizationForResource, reloadResourcesForOrganization, closeCreateResourceModal },
  ) => {
    try {
      const organizationId = selectedOrganizationForResource.value?.id
      if (organizationId) {
        await reloadResourcesForOrganization(organizationId)
      }
      closeCreateResourceModal()
    } catch (error) {
      console.error('Error handling resource creation:', error)
    }
  }

  const handleResourceUpdate = async (
    updatedResourceData,
    {
      selectedResource,
      organizationResources,
      reloadResourcesForOrganization,
      closeEditResourceModal,
    },
  ) => {
    try {
      let organizationId =
        updatedResourceData.organizationId || selectedResource.value?.organizationId

      if (!organizationId) {
        for (const [orgId, resources] of Object.entries(organizationResources.value)) {
          if (
            resources.some(
              (resource) =>
                resource.id === updatedResourceData.id ||
                resource.id === selectedResource.value?.id,
            )
          ) {
            organizationId = orgId
            break
          }
        }
      }

      if (organizationId) {
        await reloadResourcesForOrganization(organizationId)
      }

      closeEditResourceModal()
    } catch (error) {
      console.error('Error handling resource update:', error)
    }
  }

  return {
    loading,
    handleOrganizationCreate,
    handleOrganizationUpdate,
    handleResourceCreate,
    handleResourceUpdate,
  }
}
