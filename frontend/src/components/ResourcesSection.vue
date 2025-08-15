<template>
  <div class="resources-section">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2 class="text-left">Resources</h2>
    </div>

    <!-- Search Input -->
    <div class="search-container mb-3">
      <input
        v-model="searchQuery"
        type="text"
        class="form-control search-input"
        placeholder="Search by resource name..."
        @input="debouncedSearch"
      />
    </div>

    <div v-if="resources.length === 0 && !loading" class="text-muted mb-3">
      {{ searchQuery ? 'No resources found matching your search.' : 'No resources found.' }}
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
              <th>Resource ID</th>
              <th>Status</th>
              <th>Organization</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="resource in resources" :key="resource.id">
              <td>{{ resource.name }}</td>
              <td>{{ resource.sourceId }}</td>
              <td>
                <span :class="resource.withdrawn ? 'badge bg-danger' : 'badge bg-success'">
                  {{ resource.withdrawn ? 'Withdrawn' : 'Active' }}
                </span>
              </td>
              <td>{{ resource.organization.name }}</td>
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
import EditResourceModal from '@/components/modals/EditResourceModal.vue'

const adminStore = useAdminStore()

const resources = ref([])
const loading = ref(true)
const pageNumber = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageLinks = ref({})
const searchQuery = ref('')
const pageSize = ref(20)
const selectedResource = ref(null)
const showEditModal = ref(false)

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
    loadResources(searchQuery.value)
  }, 500)
}

onMounted(() => {
  loadResources()
})

async function loadResources(name = '') {
  loading.value = true
  try {
    const response = await adminStore.retrieveResourcesPaginated(
      name,
      pageNumber.value,
      pageSize.value,
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
    loadResources(searchQuery.value)
  }
}

const nextPage = () => {
  if (pageNumber.value < totalPages.value - 1) {
    pageNumber.value += 1
    loadResources(searchQuery.value)
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
  loadResources(searchQuery.value)
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

    // Call the update method from the store
    await adminStore.updateResource(resourceId, updateData)

    // Update the local resources array with the updated data
    const index = resources.value.findIndex((res) => res.id === resourceId)
    if (index !== -1) {
      // Merge the update data with the existing resource
      resources.value[index] = { ...resources.value[index], ...updateData }
    }

    closeEditModal()
  } catch (error) {
    console.error('Error updating resource:', error)
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
</style>
