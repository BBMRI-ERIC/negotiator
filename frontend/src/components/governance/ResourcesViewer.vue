<template>
  <div class="resources-viewer">
    <!-- Header with Search and Filters -->
    <div class="resources-header mb-3">
      <!-- Search and Filters Row -->
      <div class="row g-2">
        <div class="col-md-6">
          <div class="position-relative">
            <input
              v-model="searchQuery"
              type="text"
              class="form-control form-control-sm search-input"
              placeholder="Search resources by name, source ID, or description..."
            />
            <i class="bi bi-search search-icon"></i>
          </div>
        </div>
        <div class="col-md-3">
          <select v-model="statusFilter" class="form-select form-select-sm">
            <option value="">All Statuses</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        </div>
        <div class="col-md-3">
          <select v-model="sortBy" class="form-select form-select-sm">
            <option value="name">Sort by Name</option>
            <option value="sourceId">Sort by Source ID</option>
            <option value="description">Sort by Description</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-4">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading resources...</span>
      </div>
      <p class="text-muted mt-2 mb-0">Loading resources...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredResources.length === 0" class="empty-state">
      <div class="text-center py-4">
        <i class="bi bi-database text-muted" style="font-size: 2.5rem;"></i>
        <h6 class="text-muted mt-2 mb-1">
          {{ searchQuery || statusFilter ? 'No resources found matching your criteria' : 'No resources available' }}
        </h6>
        <p class="text-muted small mb-0">
          {{ searchQuery || statusFilter ? 'Try adjusting your search terms or filters' : 'This organization has no resources' }}
        </p>
      </div>
    </div>

    <!-- Resources Table -->
    <div v-else class="resources-table-container">
      <div class="table-responsive">
        <table class="table table-hover resources-table mb-0">
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
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="resource in filteredResources"
              :key="resource.id"
              class="resource-row"
            >
              <td class="py-2">
                <div class="fw-medium">{{ resource.name }}</div>
                <small class="text-muted">ID: {{ resource.id }}</small>
              </td>
              <td class="py-2">
                <code class="source-id">{{ resource.sourceId }}</code>
              </td>
              <td class="py-2">
                <div class="description-cell">
                  {{ truncateText(resource.description, 80) }}
                </div>
              </td>
              <td class="py-2">
                <div v-if="resource.contactEmail" class="small">
                  <i class="bi bi-envelope me-1"></i>
                  {{ resource.contactEmail }}
                </div>
                <span v-else class="text-muted">-</span>
              </td>
              <td class="py-2">
                <span :class="getStatusBadgeClass(resource)">
                  {{ getStatusText(resource) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Results Summary moved to bottom -->
    <div v-if="!loading && filteredResources.length > 0" class="results-summary mt-3 pt-2 border-top">
      <div class="d-flex justify-content-between align-items-center">
        <div class="text-muted small">
          Showing {{ filteredResources.length }} of {{ allResources.length }} resources
          <span v-if="searchQuery || statusFilter" class="text-info">
            (filtered)
          </span>
        </div>
        <div v-if="searchQuery" class="text-muted small">
          <i class="bi bi-search me-1"></i>
          Searching for: "{{ searchQuery }}"
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  organization: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

// Local reactive state
const searchQuery = ref('')
const statusFilter = ref('')
const sortBy = ref('name')
const sortDirection = ref('asc')

// Computed properties
const allResources = computed(() => props.organization?.resources || [])

const filteredResources = computed(() => {
  let resources = [...allResources.value]

  // Apply search filter
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    resources = resources.filter(resource =>
      resource.name?.toLowerCase().includes(query) ||
      resource.sourceId?.toLowerCase().includes(query) ||
      resource.description?.toLowerCase().includes(query)
    )
  }

  // Apply status filter
  if (statusFilter.value) {
    resources = resources.filter(resource => {
      const isActive = !resource.withdrawn
      return statusFilter.value === 'active' ? isActive : !isActive
    })
  }

  // Apply sorting
  resources.sort((a, b) => {
    let aValue = a[sortBy.value] || ''
    let bValue = b[sortBy.value] || ''

    if (typeof aValue === 'string') {
      aValue = aValue.toLowerCase()
      bValue = bValue.toLowerCase()
    }

    if (sortDirection.value === 'asc') {
      return aValue < bValue ? -1 : aValue > bValue ? 1 : 0
    } else {
      return aValue > bValue ? -1 : aValue < bValue ? 1 : 0
    }
  })

  return resources
})

// Methods
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
    return 'bi bi-arrow-down-up text-muted'
  }
  return sortDirection.value === 'asc' ? 'bi bi-arrow-up' : 'bi bi-arrow-down'
}

const truncateText = (text, maxLength) => {
  if (!text) return '-'
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

const getStatusBadgeClass = (resource) => {
  const isActive = !resource.withdrawn
  return isActive ? 'badge bg-success' : 'badge bg-secondary'
}

const getStatusText = (resource) => {
  const isActive = !resource.withdrawn
  return isActive ? 'Active' : 'Inactive'
}

// Watch for organization changes to reset filters
watch(() => props.organization, () => {
  searchQuery.value = ''
  statusFilter.value = ''
  sortBy.value = 'name'
  sortDirection.value = 'asc'
})
</script>

<style scoped>
.resources-viewer {
  padding: 1.5rem;
}

.resources-header {
  margin-bottom: 2rem;
}

.resources-header h5 {
  margin-bottom: 0;
  font-weight: 600;
  color: #2c3e50;
}

.search-input {
  padding: 0.75rem 2.75rem 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 2px solid #e8ecef;
  transition: all 0.2s ease;
}

.search-input:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.15);
}

.search-icon {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: #6c757d;
  pointer-events: none;
  font-size: 1.1rem;
}

.form-select {
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 2px solid #e8ecef;
  transition: all 0.2s ease;
  font-size: 0.9rem;
}

.form-select:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.15);
}

.resources-table-container {
  background: white;
  border-radius: 0.75rem;
  border: 1px solid #e8ecef;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  margin-bottom: 1.5rem;
}

.resources-table {
  margin-bottom: 0;
}

.resources-table thead th {
  border-bottom: 2px solid #e8ecef;
  background-color: #f8f9fa;
  font-weight: 600;
  color: #495057;
  font-size: 0.875rem;
  padding: 1.25rem 1.5rem;
  letter-spacing: 0.025em;
}

.resources-table tbody td {
  padding: 1.25rem 1.5rem;
  vertical-align: middle;
  border-bottom: 1px solid #f1f3f4;
}

.resources-table tbody tr:last-child td {
  border-bottom: none;
}

.sortable {
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s ease;
}

.sortable:hover {
  background-color: #e8ecef !important;
}

.sort-icon {
  font-size: 0.75rem;
  transition: color 0.15s ease;
  margin-left: 0.5rem;
}

.resource-row {
  transition: background-color 0.15s ease;
}

.resource-row:hover {
  background-color: #f8f9fa;
}

.source-id {
  font-size: 0.875rem;
  background-color: #f8f9fa;
  color: #495057;
  padding: 0.4rem 0.8rem;
  border-radius: 0.375rem;
  border: 1px solid #e8ecef;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  letter-spacing: 0.025em;
}

.description-cell {
  max-width: 300px;
  word-wrap: break-word;
  line-height: 1.5;
  color: #6c757d;
}

.badge {
  font-size: 0.75rem;
  padding: 0.5rem 0.875rem;
  border-radius: 0.5rem;
  font-weight: 500;
  letter-spacing: 0.025em;
}

.badge.bg-success {
  background-color: #d4edda !important;
  color: #155724 !important;
  border: 1px solid #c3e6cb;
}

.badge.bg-secondary {
  background-color: #f8f9fa !important;
  color: #6c757d !important;
  border: 1px solid #e8ecef;
}

.empty-state {
  background: white;
  border-radius: 0.75rem;
  border: 1px solid #e8ecef;
  margin: 2rem 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.empty-state .text-center {
  padding: 3rem 2rem;
}

.empty-state i {
  margin-bottom: 1rem;
  opacity: 0.6;
}

.empty-state h6 {
  margin-bottom: 1rem;
  font-weight: 500;
}

.empty-state p {
  margin-bottom: 0;
  font-size: 0.95rem;
  line-height: 1.5;
}

/* Results Summary Styling */
.resources-table-container + .d-flex {
  margin-top: 1.5rem;
  padding: 0 0.25rem;
}

.text-muted {
  font-size: 0.875rem;
  color: #6c757d !important;
}

.text-info {
  font-weight: 500;
  color: #0dcaf0 !important;
}

/* Resource count badge in header */
.badge.bg-secondary.ms-2 {
  background-color: #6c757d !important;
  color: white !important;
  padding: 0.375rem 0.75rem;
  border-radius: 1rem;
  font-size: 0.8rem;
  font-weight: 500;
  margin-left: 0.75rem !important;
}

/* Contact email styling */
.resources-table tbody td:nth-child(4) {
  font-size: 0.875rem;
}

.resources-table tbody td:nth-child(4) i {
  color: #6c757d;
  margin-right: 0.5rem;
}

/* Resource name styling */
.fw-medium {
  font-weight: 500 !important;
  color: #2c3e50;
  margin-bottom: 0.25rem;
}

.fw-medium + small {
  color: #6c757d;
  font-size: 0.8rem;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .resources-viewer {
    padding: 1rem;
  }

  .resources-header {
    margin-bottom: 1.5rem;
  }

  .resources-table thead th,
  .resources-table tbody td {
    padding: 1rem;
  }

  .empty-state .text-center {
    padding: 2rem 1rem;
  }
}

@media (max-width: 576px) {
  .resources-viewer {
    padding: 0.75rem;
  }

  .resources-table thead th,
  .resources-table tbody td {
    padding: 0.75rem 0.5rem;
    font-size: 0.85rem;
  }

  .source-id {
    font-size: 0.8rem;
    padding: 0.3rem 0.6rem;
  }

  .description-cell {
    max-width: 200px;
  }
}
</style>
