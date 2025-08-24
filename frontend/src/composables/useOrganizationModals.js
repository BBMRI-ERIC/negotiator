import { ref } from 'vue'
import { Modal } from 'bootstrap'

export function useOrganizationModals() {
  // Modal state
  const selectedOrganization = ref(null)
  const selectedOrganizationForResource = ref(null)
  const selectedResource = ref(null)
  const showEditModal = ref(false)
  const showCreateModal = ref(false)
  const showCreateResourceModal = ref(false)
  const showEditResourceModal = ref(false)

  // Organization modal methods
  const openCreateModal = () => {
    showCreateModal.value = true
    const modalElement = document.getElementById('createOrganizationModal')
    if (modalElement) {
      const modal = new Modal(modalElement)
      modal.show()
    }
  }

  const closeCreateModal = () => {
    showCreateModal.value = false
    const modalElement = document.getElementById('createOrganizationModal')
    if (modalElement) {
      const modal = Modal.getInstance(modalElement)
      if (modal) {
        modal.hide()
      }
    }
  }

  const openEditModal = (organization) => {
    selectedOrganization.value = { ...organization }
    showEditModal.value = true
    const modalElement = document.getElementById('editOrganizationModal')
    if (modalElement) {
      const modal = new Modal(modalElement)
      modal.show()
    }
  }

  const closeEditModal = () => {
    showEditModal.value = false
    selectedOrganization.value = null
    const modalElement = document.getElementById('editOrganizationModal')
    if (modalElement) {
      const modal = Modal.getInstance(modalElement)
      if (modal) {
        modal.hide()
      }
    }
  }

  // Resource modal methods
  const openCreateResourceModal = (organization) => {
    selectedOrganizationForResource.value = organization
    showCreateResourceModal.value = true
    const modalElement = document.getElementById('createResourceModal')
    if (modalElement) {
      const modal = new Modal(modalElement)
      modal.show()
    }
  }

  const closeCreateResourceModal = () => {
    showCreateResourceModal.value = false
    selectedOrganizationForResource.value = null
    const modalElement = document.getElementById('createResourceModal')
    if (modalElement) {
      const modal = Modal.getInstance(modalElement)
      if (modal) {
        modal.hide()
      }
    }
  }

  const openEditResourceModal = (resource) => {
    selectedResource.value = { ...resource }
    showEditResourceModal.value = true
    const modalElement = document.getElementById('editResourceModal')
    if (modalElement) {
      const modal = new Modal(modalElement)
      modal.show()
    }
  }

  const closeEditResourceModal = () => {
    showEditResourceModal.value = false
    selectedResource.value = null
    const modalElement = document.getElementById('editResourceModal')
    if (modalElement) {
      const modal = Modal.getInstance(modalElement)
      if (modal) {
        modal.hide()
      }
    }
  }

  return {
    // State
    selectedOrganization,
    selectedOrganizationForResource,
    selectedResource,
    showEditModal,
    showCreateModal,
    showCreateResourceModal,
    showEditResourceModal,

    // Methods
    openCreateModal,
    closeCreateModal,
    openEditModal,
    closeEditModal,
    openCreateResourceModal,
    closeCreateResourceModal,
    openEditResourceModal,
    closeEditResourceModal
  }
}
