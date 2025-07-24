<template>
  <div class="organizations-section">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2 class="text-left">Organizations</h2>
      <button class="btn btn-primary" @click="openCreateModal" :disabled="loading">
        <i class="bi bi-plus-circle me-2"></i>
        Create Organization
      </button>
    </div>

    <!-- Search Input -->
    <div class="search-container mb-3">
      <input
        v-model="searchQuery"
        type="text"
        class="form-control search-input"
        placeholder="Search by organization name..."
        @input="debouncedSearch"
      />
    </div>

    <div v-if="organizations.length === 0 && !loading" class="text-muted mb-3">
      {{ searchQuery ? 'No organizations found matching your search.' : 'No organizations found.' }}
    </div>

    <div v-else class="table-container">
      <!-- Loading Spinner -->
      <div v-if="loading" class="text-center my-4">
        <div class="spinner-border" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </div>

      <div v-else>
        <table class="table table-hover">
          <thead>
            <tr>
              <th>Organization Name</th>
              <th>External ID</th>
              <th>Contact Email</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="organization in organizations" :key="organization.id">
              <td>{{ organization.name }}</td>
              <td>{{ organization.externalId }}</td>
              <td>{{ organization.contactEmail || 'N/A' }}</td>
              <td>
                <span :class="organization.withdrawn ? 'badge bg-danger' : 'badge bg-success'">
                  {{ organization.withdrawn ? 'Withdrawn' : 'Active' }}
                </span>
              </td>
              <td>
                <button
                  class="btn btn-sm btn-outline-primary"
                  @click="editOrganization(organization)"
                  :disabled="loading"
                >
                  <i class="bi bi-pencil"></i> Edit
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div class="pagination">
          <button @click="previousPage" :disabled="pageNumber === 0 || loading" class="page-button">
            ‹ Prev
          </button>
          <span class="page-info">Page {{ pageNumber + 1 }} of {{ totalPages }}</span>
          <button
            @click="nextPage"
            :disabled="pageNumber === totalPages - 1 || loading"
            class="page-button"
          >
            Next ›
          </button>
          <input
            v-model.number="pageSize"
            @change="resetPage"
            type="number"
            min="1"
            max="100"
            class="page-size-input"
            placeholder="Page size"
          />
        </div>
      </div>
    </div>

    <!-- Edit Organization Modal -->
    <EditOrganizationModal
      modal-id="editOrganizationModal"
      :organization="selectedOrganization"
      :shown="showEditModal"
      @update="handleOrganizationUpdate"
      @close="closeEditModal"
    />

    <!-- Create Organization Modal -->
    <CreateOrganizationModal
      modal-id="createOrganizationModal"
      :shown="showCreateModal"
      @create="handleOrganizationCreate"
      @close="closeCreateModal"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Modal } from 'bootstrap'
import { useAdminStore } from '@/store/admin'
import EditOrganizationModal from '@/components/modals/EditOrganizationModal.vue'
import CreateOrganizationModal from '@/components/modals/CreateOrganizationModal.vue'

const adminStore = useAdminStore()

const organizations = ref([])
const loading = ref(true)
const pageNumber = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageLinks = ref({})
const pageSize = ref(20)
const selectedOrganization = ref(null)
const showEditModal = ref(false)
const showCreateModal = ref(false)
const searchQuery = ref('')

// Debounce timeout reference
let searchTimeout = null

const debouncedSearch = () => {
  // Clear previous timeout
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }

  // Set new timeout for 500ms delay
  searchTimeout = setTimeout(() => {
    pageNumber.value = 0
    loadOrganizations(searchQuery.value)
  }, 500)
}

onMounted(() => {
  loadOrganizations()
})

async function loadOrganizations(name = '') {
  loading.value = true
  try {
    const response = await adminStore.retrieveOrganizationsPaginated(
      pageNumber.value,
      pageSize.value,
      name,
    )
    organizations.value = response?._embedded?.organizations ?? []
    pageLinks.value = response._links || {}
    pageNumber.value = response.page?.number ?? 0
    totalPages.value = response.page?.totalPages ?? 0
    totalElements.value = response.page?.totalElements ?? 0
  } catch (error) {
    console.error('Error loading organizations:', error)
    organizations.value = []
  } finally {
    loading.value = false
  }
}

const previousPage = () => {
  if (pageNumber.value > 0) {
    pageNumber.value -= 1
    loadOrganizations(searchQuery.value)
  }
}

const nextPage = () => {
  if (pageNumber.value < totalPages.value - 1) {
    pageNumber.value += 1
    loadOrganizations(searchQuery.value)
  }
}

const resetPage = () => {
  if (pageSize.value < 1) {
    pageSize.value = 20
  }
  if (pageSize.value > 100) {
    pageSize.value = 100
  }
  pageNumber.value = 0
  loadOrganizations(searchQuery.value)
}

const editOrganization = (organization) => {
  selectedOrganization.value = { ...organization }
  showEditModal.value = true

  // Use Bootstrap modal to show the modal
  const modalElement = document.getElementById('editOrganizationModal')
  if (modalElement) {
    const modal = new Modal(modalElement)
    modal.show()
  }
}

const closeEditModal = () => {
  showEditModal.value = false
  selectedOrganization.value = null

  // Hide the Bootstrap modal
  const modalElement = document.getElementById('editOrganizationModal')
  if (modalElement) {
    const modal = Modal.getInstance(modalElement)
    if (modal) {
      modal.hide()
    }
  }
}

const handleOrganizationUpdate = async ({ organizationId, updateData }) => {
  try {
    loading.value = true

    console.log('Frontend: Updating organization with ID:', organizationId)
    console.log('Frontend: Update data being sent:', JSON.stringify(updateData, null, 2))

    // Call the update method from the store
    const updatedOrganization = await adminStore.updateOrganization(organizationId, updateData)

    console.log('Frontend: Update successful, response:', updatedOrganization)

    // Update the local organizations array with the updated data
    const index = organizations.value.findIndex((org) => org.id === organizationId)
    if (index !== -1) {
      // Merge the update data with the existing organization
      organizations.value[index] = { ...organizations.value[index], ...updateData }
    }

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

const openCreateModal = () => {
  showCreateModal.value = true

  // Use Bootstrap modal to show the modal
  const modalElement = document.getElementById('createOrganizationModal')
  if (modalElement) {
    const modal = new Modal(modalElement)
    modal.show()
  }
}

const closeCreateModal = () => {
  showCreateModal.value = false

  // Hide the Bootstrap modal
  const modalElement = document.getElementById('createOrganizationModal')
  if (modalElement) {
    const modal = Modal.getInstance(modalElement)
    if (modal) {
      modal.hide()
    }
  }
}

const handleOrganizationCreate = async (newOrganization) => {
  try {
    loading.value = true

    console.log(
      'Frontend: Creating new organization with data:',
      JSON.stringify(newOrganization, null, 2),
    )

    // Call the create method from the store
    const createdOrganization = await adminStore.createOrganization(newOrganization)

    console.log('Frontend: Organization created successfully, response:', createdOrganization)

    // Add the new organization to the local organizations array
    organizations.value.push(createdOrganization)

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
</script>

<style scoped>
.organizations-section {
  background: #ffffff;
}

.text-muted {
  font-size: 1rem;
  color: #6c757d;
  margin-bottom: 1rem;
}

.table-container {
  margin-top: 1rem;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table thead th {
  font-size: 0.9rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #6c757d;
  padding: 1rem;
  border-bottom: 2px solid #e8ecef;
  cursor: pointer;
}

.table tbody tr {
  transition: background-color 0.2s ease;
}

.table tbody td {
  font-size: 0.95rem;
  color: #6c757d;
  padding: 1rem;
  vertical-align: middle;
  border-bottom: 1px solid #e8ecef;
}

.table tbody tr:hover {
  background-color: #f8f9fa;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 1rem;
}

.page-button {
  padding: 0.5rem 1rem;
  margin: 0 0.5rem;
  background-color: #f8f9fa;
  border: 1px solid #e8ecef;
  cursor: pointer;
  font-size: 0.95rem;
  border-radius: 0.375rem;
}

.page-button:disabled {
  background-color: #e8ecef;
  color: #6c757d;
  cursor: not-allowed;
}

.page-button:not(:disabled):hover {
  background-color: #e8ecef;
}

.page-info {
  margin: 0 1rem;
  font-size: 0.95rem;
  color: #6c757d;
}

.page-size-input {
  padding: 0.5rem;
  margin-left: 1rem;
  width: 80px;
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
  font-size: 0.9rem;
}

.spinner-border {
  color: #0d6efd;
}

.badge {
  font-size: 0.8rem;
  padding: 0.375rem 0.75rem;
  border-radius: 0.375rem;
}

.btn-outline-primary {
  color: #0d6efd;
  border-color: #0d6efd;
}

.btn-outline-primary:hover {
  background-color: #0d6efd;
  color: #ffffff;
}

.search-container {
  max-width: 400px;
}

.search-input {
  font-size: 0.95rem;
  padding: 0.75rem 1rem;
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
}

.search-input:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}
</style>
