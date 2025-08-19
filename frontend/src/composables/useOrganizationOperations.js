import { ref } from 'vue'
import { useAdminStore } from '@/store/admin'

export function useOrganizationOperations() {
  const adminStore = useAdminStore()
  const loading = ref(false)

  const handleOrganizationCreate = async (newOrganization, { loadOrganizations, closeCreateModal }) => {
    try {
      loading.value = true
      console.log('Frontend: Creating new organization with data:', JSON.stringify(newOrganization, null, 2))

      const createdOrganization = await adminStore.createOrganization(newOrganization)
      console.log('Frontend: Organization created successfully, response:', createdOrganization)

      await loadOrganizations()
      closeCreateModal()
    } catch (error) {
      console.error('Frontend: Error creating organization:', error)
      console.error('Frontend: Error details:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
      })
    } finally {
      loading.value = false
    }
  }

  const handleOrganizationUpdate = async ({ organizationId, updateData }, { loadOrganizations, closeEditModal }) => {
    try {
      loading.value = true
      console.log('Frontend: Updating organization with ID:', organizationId)
      console.log('Frontend: Update data being sent:', JSON.stringify(updateData, null, 2))

      const updatedOrganization = await adminStore.updateOrganization(organizationId, updateData)
      console.log('Frontend: Update successful, response:', updatedOrganization)

      await loadOrganizations()
      closeEditModal()
    } catch (error) {
      console.error('Frontend: Error updating organization:', error)
      console.error('Frontend: Error details:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
      })
    } finally {
      loading.value = false
    }
  }

  const handleResourceCreate = async (createdResourceData, { selectedOrganizationForResource, reloadResourcesForOrganization, closeCreateResourceModal }) => {
    try {
      console.log('Resource created successfully:', createdResourceData)

      const organizationId = selectedOrganizationForResource.value?.id
      if (organizationId) {
        await reloadResourcesForOrganization(organizationId)
      }

      closeCreateResourceModal()
    } catch (error) {
      console.error('Error handling resource creation:', error)
    }
  }

  const handleResourceUpdate = async (updatedResourceData, { selectedResource, organizationResources, reloadResourcesForOrganization, closeEditResourceModal }) => {
    try {
      console.log('Resource updated successfully:', updatedResourceData)

      // Find the organization ID from the updated resource or the selected resource
      let organizationId = updatedResourceData.organizationId || selectedResource.value?.organizationId

      // If we still don't have the organization ID, search through all loaded resources
      if (!organizationId) {
        for (const [orgId, resources] of Object.entries(organizationResources.value)) {
          if (resources.some(resource => resource.id === updatedResourceData.id || resource.id === selectedResource.value?.id)) {
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
    handleResourceUpdate
  }
}
