<template>
  <div class="resources-management">

    <!-- Header with Search and Actions -->
    <div class="resources-header">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="d-flex align-items-center">
          <h5 class="mb-0">Resources Management</h5>
          <span class="badge bg-secondary ms-2">{{ filteredResources.length }} resources</span>
        </div>
        <button
          type="button"
          class="btn btn-primary btn-sm"
          @click="showAddResourceModal = true"
        >
          <i class="bi bi-plus-circle me-2"></i>
          Add Resource
        </button>
      </div>

      <!-- Search and Filters Row -->
      <div class="row mb-3">
        <div class="col-md-6">
          <div class="position-relative">
            <input
              v-model="searchQuery"
              type="text"
              class="form-control search-input"
              placeholder="Search resources by name, source ID, or description..."
            />
            <i class="bi bi-search search-icon"></i>
          </div>
        </div>
        <div class="col-md-3">
          <select v-model="statusFilter" class="form-select">
            <option value="">All Statuses</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        </div>
        <div class="col-md-3">
          <select v-model="sortBy" class="form-select">
            <option value="name">Sort by Name</option>
            <option value="sourceId">Sort by Source ID</option>
            <option value="description">Sort by Description</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading resources...</span>
      </div>
      <p class="text-muted mt-2">Loading resources...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredResources.length === 0" class="empty-state">
      <div class="text-center py-5">
        <i class="bi bi-database text-muted" style="font-size: 3rem;"></i>
        <h6 class="text-muted mt-3">
          {{ searchQuery || statusFilter ? 'No resources found matching your criteria' : 'No resources yet' }}
        </h6>
        <p class="text-muted">
          {{ searchQuery || statusFilter ? 'Try adjusting your search terms or filters' : 'Get started by adding your first resource' }}
        </p>
        <button
          v-if="!searchQuery && !statusFilter"
          type="button"
          class="btn btn-outline-primary"
          @click="showAddResourceModal = true"
        >
          <i class="bi bi-plus-circle me-2"></i>
          Add First Resource
        </button>
      </div>
    </div>

    <!-- Resources Table -->
    <div v-else class="resources-table-container">
      <div class="table-responsive">
        <table class="table table-hover resources-table">
          <thead class="table-light">
            <tr>
              <th @click="sortColumn('name')" class="sortable">
                <div class="d-flex align-items-center">
                  Name
                  <i :class="getSortIcon('name')" class="ms-1 sort-icon"></i>
                </div>
              </th>
              <th @click="sortColumn('sourceId')" class="sortable">
                <div class="d-flex align-items-center">
                  Source ID
                  <i :class="getSortIcon('sourceId')" class="ms-1 sort-icon"></i>
                </div>
              </th>
              <th>Description</th>
              <th>Contact</th>
              <th>Status</th>
              <th class="text-end">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="resource in filteredResources"
              :key="resource.id"
              class="resource-row"
              @click="selectResource(resource)"
            >
              <td>
                <div class="fw-medium">{{ resource.name }}</div>
                <small class="text-muted">ID: {{ resource.id }}</small>
              </td>
              <td>
                <code class="source-id">{{ resource.sourceId }}</code>
              </td>
              <td>
                <div class="description-cell">
                  {{ truncateText(resource.description, 80) }}
                </div>
              </td>
              <td>
                <div v-if="resource.contactEmail">
                  <i class="bi bi-envelope me-1"></i>
                  {{ resource.contactEmail }}
                </div>
                <span v-else class="text-muted">-</span>
              </td>
              <td>
                <span :class="getStatusBadgeClass(resource)">
                  {{ getStatusText(resource) }}
                </span>
              </td>
              <td class="text-end">
                <div class="btn-group btn-group-sm">
                  <button
                    type="button"
                    class="btn btn-outline-primary"
                    @click.stop="editResource(resource)"
                    title="Edit Resource"
                  >
                    <i class="bi bi-pencil"></i>
                  </button>
                  <button
                    type="button"
                    class="btn btn-outline-info"
                    @click.stop="viewResource(resource)"
                    title="View Details"
                  >
                    <i class="bi bi-eye"></i>
                  </button>
                  <button
                    type="button"
                    class="btn btn-outline-danger"
                    @click.stop="deleteResource(resource)"
                    title="Delete Resource"
                  >
                    <i class="bi bi-trash"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Results Summary -->
      <div class="d-flex justify-content-between align-items-center mt-3">
        <div class="text-muted">
          Showing {{ filteredResources.length }} of {{ allResources.length }} resources
          <span v-if="searchQuery || statusFilter" class="text-info">
            (filtered)
          </span>
        </div>
        <div v-if="searchQuery" class="text-muted">
          <i class="bi bi-search me-1"></i>
          Searching for: "{{ searchQuery }}"
        </div>
      </div>
    </div>

    <!-- Add Resource Modal -->
    <AddResourceModal
      v-if="showAddResourceModal"
      :organization-id="organizationId"
      @resource-added="handleResourceAdded"
      @close="showAddResourceModal = false"
    />

    <!-- Edit Resource Modal -->
    <EditResourceModal
      v-if="selectedResource && showEditModal"
      :resource="selectedResource"
      @resource-updated="handleResourceUpdated"
      @close="closeEditModal"
    />

    <!-- View Resource Modal -->
    <ViewResourceModal
      v-if="selectedResource && showViewModal"
      :resource="selectedResource"
      @close="closeViewModal"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useAdminStore } from '@/store/admin'
import AddResourceModal from '@/components/modals/AddResourceModal.vue'
import EditResourceModal from '@/components/modals/EditResourceModal.vue'
import ViewResourceModal from '@/components/modals/ViewResourceModal.vue'

const props = defineProps({
  organizationId: {
    type: [String, Number],
    required: true,
  },
  organization: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['resource-added', 'resource-updated', 'resource-deleted'])

const adminStore = useAdminStore()

// Reactive state
const allResources = ref([])
const loading = ref(false)
const searchQuery = ref('')
const statusFilter = ref('')
const sortBy = ref('name')
const sortDirection = ref('asc')

// Modal states
const showAddResourceModal = ref(false)
const showEditModal = ref(false)
const showViewModal = ref(false)
const selectedResource = ref(null)

// Computed properties
const filteredResources = computed(() => {
  let filtered = [...allResources.value]

  // Apply search filter
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(resource =>
      resource.name?.toLowerCase().includes(query) ||
      resource.sourceId?.toLowerCase().includes(query) ||
      resource.description?.toLowerCase().includes(query) ||
      resource.contactEmail?.toLowerCase().includes(query)
    )
  }

  // Apply status filter
  if (statusFilter.value) {
    filtered = filtered.filter(resource => {
      if (statusFilter.value === 'active') return resource.active !== false
      if (statusFilter.value === 'inactive') return resource.active === false
      return true
    })
  }

  // Apply sorting
  filtered.sort((a, b) => {
    const aValue = a[sortBy.value] || ''
    const bValue = b[sortBy.value] || ''

    let comparison
    if (typeof aValue === 'string' && typeof bValue === 'string') {
      comparison = aValue.localeCompare(bValue)
    } else {
      comparison = aValue < bValue ? -1 : aValue > bValue ? 1 : 0
    }

    return sortDirection.value === 'desc' ? -comparison : comparison
  })

  return filtered
})

// Methods
const loadResources = async () => {
  loading.value = true

  try {
    // If organization prop has resources, use them directly
    if (props.organization?.resources) {
      allResources.value = props.organization.resources
    } else {
      // Otherwise fetch from API (fallback)
      const response = await adminStore.retrieveResources()
      // Filter resources by organization if needed
      allResources.value = response.filter(resource =>
        resource.organizationId === props.organizationId
      )
    }
  } catch (error) {
    console.error('Error loading resources:', error)
    allResources.value = []
  } finally {
    loading.value = false
  }
}

const sortColumn = (column) => {
  if (sortBy.value === column) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = column
    sortDirection.value = 'asc'
  }
}

const getSortIcon = (column) => {
  if (sortBy.value !== column) {
    return 'bi bi-arrow-up-down text-muted'
  }
  return sortDirection.value === 'asc'
    ? 'bi bi-arrow-up text-primary'
    : 'bi bi-arrow-down text-primary'
}

const selectResource = (resource) => {
  selectedResource.value = resource
}

const editResource = (resource) => {
  selectedResource.value = resource
  showEditModal.value = true
}

const viewResource = (resource) => {
  selectedResource.value = resource
  showViewModal.value = true
}

const deleteResource = async (resource) => {
  if (confirm(`Are you sure you want to delete the resource "${resource.name}"?`)) {
    try {
      await adminStore.deleteResource(resource.id)
      // Remove from local array
      const index = allResources.value.findIndex(r => r.id === resource.id)
      if (index > -1) {
        allResources.value.splice(index, 1)
      }
      emit('resource-deleted')
    } catch (error) {
      console.error('Error deleting resource:', error)
    }
  }
}

const closeEditModal = () => {
  showEditModal.value = false
  selectedResource.value = null
}

const closeViewModal = () => {
  showViewModal.value = false
  selectedResource.value = null
}

const handleResourceAdded = (newResource) => {
  showAddResourceModal.value = false
  // Add to local array
  if (newResource) {
    allResources.value.push(newResource)
  } else {
    // Reload if we don't have the new resource object
    loadResources()
  }
  emit('resource-added')
}

const handleResourceUpdated = (updatedResource) => {
  closeEditModal()
  // Update in local array
  if (updatedResource) {
    const index = allResources.value.findIndex(r => r.id === updatedResource.id)
    if (index > -1) {
      allResources.value[index] = updatedResource
    }
  } else {
    // Reload if we don't have the updated resource object
    loadResources()
  }
  emit('resource-updated')
}

const truncateText = (text, maxLength) => {
  if (!text) return '-'
  return text.length > maxLength ? `${text.substring(0, maxLength)}...` : text
}

const getStatusBadgeClass = (resource) => {
  const baseClass = 'badge '
  return baseClass + (resource.active !== false ? 'bg-success' : 'bg-secondary')
}

const getStatusText = (resource) => {
  return resource.active !== false ? 'Active' : 'Inactive'
}

// Lifecycle
onMounted(() => {
  loadResources()
})

// Watch for organization changes
watch(() => props.organization, () => {
  searchQuery.value = ''
  statusFilter.value = ''
  loadResources()
}, { deep: true })

// Watch for organizationId changes
watch(() => props.organizationId, () => {
  searchQuery.value = ''
  statusFilter.value = ''
  loadResources()
})
</script>

<style scoped>
.resources-management {
  background: #ffffff;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.debug-info {
  border: 1px solid #e8ecef;
  font-family: 'Courier New', monospace;
}

.debug-info pre {
  margin: 0;
  font-size: 0.75rem;
  max-height: 200px;
  overflow-y: auto;
  background: #ffffff;
  padding: 0.5rem;
  border-radius: 0.25rem;
  border: 1px solid #e8ecef;
}

.resources-header {
  flex-shrink: 0;
  padding: 1.5rem;
  border-bottom: 1px solid #e8ecef;
}

.search-input {
  padding-left: 2.5rem;
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
  font-size: 0.95rem;
}

.search-input:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: #6c757d;
  pointer-events: none;
}

.form-select {
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
  font-size: 0.95rem;
}

.form-select:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.resources-table-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 1.5rem 1.5rem;
}

.resources-table {
  margin-bottom: 0;
  width: 100%;
  border-collapse: collapse;
}

.resources-table thead th {
  font-size: 0.9rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #6c757d;
  padding: 1rem;
  border-bottom: 2px solid #e8ecef;
  background-color: #f8f9fa;
  position: sticky;
  top: 0;
  z-index: 10;
}

.resources-table th.sortable {
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s ease;
}

.resources-table th.sortable:hover {
  background-color: #e9ecef;
}

.sort-icon {
  font-size: 0.75rem;
  transition: color 0.2s ease;
}

.resources-table tbody tr {
  transition: background-color 0.2s ease;
}

.resources-table tbody tr:hover {
  background-color: #f8f9fa;
}

.resources-table tbody td {
  font-size: 0.95rem;
  color: #495057;
  padding: 1rem;
  vertical-align: middle;
  border-bottom: 1px solid #e8ecef;
}

.resource-row {
  cursor: pointer;
}

.source-id {
  background-color: #e9ecef;
  color: #495057;
  font-size: 0.875rem;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  font-family: 'Courier New', monospace;
}

.description-cell {
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.badge {
  font-size: 0.75rem;
  padding: 0.375rem 0.75rem;
  border-radius: 0.375rem;
}

.btn {
  font-weight: 500;
  border-radius: 0.375rem;
  transition: all 0.2s ease;
}

.btn-primary {
  background-color: #0d6efd;
  border-color: #0d6efd;
}

.btn-outline-primary {
  color: #0d6efd;
  border-color: #0d6efd;
}

.btn-outline-primary:hover {
  background-color: #0d6efd;
  color: #ffffff;
}

.btn-outline-info {
  color: #0dcaf0;
  border-color: #0dcaf0;
}

.btn-outline-info:hover {
  background-color: #0dcaf0;
  color: #000000;
}

.btn-outline-danger {
  color: #dc3545;
  border-color: #dc3545;
}

.btn-outline-danger:hover {
  background-color: #dc3545;
  color: #ffffff;
}

.btn-group-sm > .btn {
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
}

.spinner-border {
  color: #0d6efd;
}

.text-muted {
  font-size: 0.95rem;
  color: #6c757d;
}

.text-info {
  color: #0dcaf0 !important;
}

@media (max-width: 768px) {
  .resources-header {
    padding: 1rem;
  }

  .resources-table-container {
    padding: 0 1rem 1rem;
  }

  .resources-table thead th {
    padding: 0.75rem 0.5rem;
    font-size: 0.85rem;
  }

  .resources-table tbody td {
    padding: 0.75rem 0.5rem;
    font-size: 0.9rem;
  }

  .description-cell {
    max-width: 150px;
  }

  .btn-group {
    flex-direction: column;
    width: 100%;
  }

  .btn-group-sm > .btn {
    margin-bottom: 0.25rem;
    width: 100%;
  }

  .search-input {
    margin-bottom: 0.5rem;
  }
}

@media (max-width: 576px) {
  .resources-header .row > div {
    margin-bottom: 0.5rem;
  }

  .resources-table thead th {
    padding: 0.5rem 0.25rem;
    font-size: 0.8rem;
  }

  .resources-table tbody td {
    padding: 0.5rem 0.25rem;
    font-size: 0.85rem;
  }

  .description-cell {
    max-width: 100px;
  }
}
</style>
