<template>
  <div class="resources-section">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2 class="text-left">Resources</h2>
    </div>

    <!-- Filters Section -->
    <div class="filters-container mb-4">
      <div class="row g-3">
        <!-- Name Search -->
        <div class="col-md-6">
          <label for="nameSearch" class="form-label">Resource Name</label>
          <input
            id="nameSearch"
            v-model="filters.name"
            type="text"
            class="form-control"
            placeholder="Search by name..."
            @input="debouncedSearch"
          />
        </div>

        <!-- Source ID Search -->
        <div class="col-md-5">
          <label for="sourceIdSearch" class="form-label">Source ID</label>
          <input
            id="sourceIdSearch"
            v-model="filters.sourceId"
            type="text"
            class="form-control"
            placeholder="Search by source ID..."
            @input="debouncedSearch"
          />
        </div>

        <!-- Clear Filters Button -->
        <div class="col-md-1 d-flex align-items-end">
          <button
            class="btn btn-outline-secondary"
            @click="clearFilters"
            :disabled="loading"
            title="Clear all filters"
          >
            <i class="bi bi-x-circle"></i>
          </button>
        </div>
      </div>
    </div>

    <div v-if="resources.length === 0 && !loading" class="text-muted mb-3">
      {{ getNoResultsMessage() }}
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
              <th>Resource Name</th>
              <th>Source ID</th>
              <th>Organization</th>
              <th>Contact Email</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="resource in resources" :key="resource.id">
              <td>{{ resource.name }}</td>
              <td>{{ resource.sourceId }}</td>
              <td>
                <div v-if="resource.organization">
                  {{ resource.organization.name }}
                </div>
                <span v-else class="text-muted">N/A</span>
              </td>
              <td>{{ resource.contactEmail || 'N/A' }}</td>
              <td>
                <UiBadge :color="resource.withdrawn ? 'bg-danger' : 'bg-success'">
                  {{ resource.withdrawn ? 'Withdrawn' : 'Active' }}
                </UiBadge>
              </td>
              <td>
                <button
                  class="btn btn-sm btn-outline-primary"
                  @click="editResource(resource)"
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

    <!-- Edit Resource Modal -->
    <EditResourceModal
      modal-id="editResourceModal"
      :resource="selectedResource"
      :shown="showEditModal"
      @update="handleResourceUpdate"
      @close="closeEditModal"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Modal } from 'bootstrap'
import { useAdminStore } from '@/store/admin'
import EditResourceModal from '@/components/governance/EditResourceModal.vue'
import UiBadge from '@/components/ui/UiBadge.vue'

const adminStore = useAdminStore()

const resources = ref([])
const loading = ref(true)
const pageNumber = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageLinks = ref({})
const pageSize = ref(20)
const selectedResource = ref(null)
const showEditModal = ref(false)

// Filters state - only using filters supported by ResourceFilterDTO
const filters = ref({
  name: '',
  sourceId: '',
})

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
    loadResources()
  }, 500)
}

onMounted(() => {
  loadResources()
})

async function loadResources() {
  loading.value = true
  try {
    // Build filters object for the API - only use supported filters
    const apiFilters = {}

    // Add name filter if provided
    if (filters.value.name && filters.value.name.trim()) {
      apiFilters.name = filters.value.name.trim()
    }

    // Add sourceId filter if provided
    if (filters.value.sourceId && filters.value.sourceId.trim()) {
      apiFilters.sourceId = filters.value.sourceId.trim()
    }

    const response = await adminStore.retrieveResourcesPaginated(
      pageNumber.value,
      pageSize.value,
      apiFilters,
    )
    resources.value = response?._embedded?.resources ?? []
    pageLinks.value = response._links || {}
    pageNumber.value = response.page?.number ?? 0
    totalPages.value = response.page?.totalPages ?? 0
    totalElements.value = response.page?.totalElements ?? 0
  } catch (error) {
    console.error('Error loading resources:', error)
    resources.value = []
  } finally {
    loading.value = false
  }
}

const previousPage = () => {
  if (pageNumber.value > 0) {
    pageNumber.value -= 1
    loadResources()
  }
}

const nextPage = () => {
  if (pageNumber.value < totalPages.value - 1) {
    pageNumber.value += 1
    loadResources()
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
  loadResources()
}

const clearFilters = () => {
  filters.value = {
    name: '',
    sourceId: '',
  }
  pageNumber.value = 0
  loadResources()
}

const getNoResultsMessage = () => {
  if (loading.value) {
    return 'Loading resources...'
  }

  const hasSearchFilters = filters.value.name || filters.value.sourceId

  if (hasSearchFilters) {
    return 'No resources found matching your search criteria.'
  }

  return 'No resources found.'
}

const editResource = (resource) => {
  selectedResource.value = { ...resource }
  showEditModal.value = true

  // Use Bootstrap modal to show the modal
  const modalElement = document.getElementById('editResourceModal')
  if (modalElement) {
    const modal = new Modal(modalElement)
    modal.show()
  }
}

const closeEditModal = () => {
  showEditModal.value = false
  selectedResource.value = null

  // Hide the Bootstrap modal
  const modalElement = document.getElementById('editResourceModal')
  if (modalElement) {
    const modal = Modal.getInstance(modalElement)
    if (modal) {
      modal.hide()
    }
  }
}

const handleResourceUpdate = async ({ resourceId, updateData }) => {
  try {
    loading.value = true

    console.log('Frontend: Updating resource with ID:', resourceId)
    console.log('Frontend: Update data being sent:', JSON.stringify(updateData, null, 2))

    // Call the update method from the store
    const updatedResource = await adminStore.updateResource(resourceId, updateData)

    console.log('Frontend: Update successful, response:', updatedResource)

    // Reload resources to ensure we get the latest data and respect current filters
    await loadResources()

    closeEditModal()
  } catch (error) {
    console.error('Frontend: Error updating resource:', error)
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
.resources-section {
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
}

.filters-container {
  background-color: #f8f9fa;
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
  padding: 1rem;
  margin-bottom: 1.5rem;
}

.form-label {
  font-size: 0.9rem;
  font-weight: 500;
  color: #495057;
  margin-bottom: 0.5rem;
}

.form-control {
  padding: 0.75rem;
  font-size: 0.95rem;
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
}

.form-control:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.btn-outline-secondary {
  color: #6c757d;
  border-color: #6c757d;
  background-color: transparent;
}

.btn-outline-secondary:hover {
  color: #ffffff;
  background-color: #6c757d;
  border-color: #6c757d;
}

.btn-outline-secondary:disabled {
  color: #6c757d;
  background-color: transparent;
  border-color: #6c757d;
  opacity: 0.65;
  cursor: not-allowed;
}

.badge {
  padding: 0.5rem 0.75rem;
  font-size: 0.8rem;
  font-weight: 500;
  border-radius: 0.375rem;
}

.bg-success {
  background-color: #198754 !important;
  color: #ffffff;
}

.bg-danger {
  background-color: #dc3545 !important;
  color: #ffffff;
}
</style>
