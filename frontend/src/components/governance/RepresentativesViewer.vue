<template>
  <div class="representatives-viewer">
    <!-- Header Section -->
    <div class="representatives-header mb-4">
      <div class="d-flex align-items-center justify-content-between">
        <div>
          <h5 class="mb-1">Resource Representatives</h5>
          <p class="text-muted mb-0">
            People authorized to manage and respond to requests for this resource
          </p>
        </div>
        <div v-if="representatives?.length" class="text-muted">
          {{ representatives.length }} {{ representatives.length === 1 ? 'representative' : 'representatives' }}
        </div>
      </div>
    </div>

    <!-- User Search Section -->
    <div class="user-search-section mb-4">
      <div class="card">
        <div class="card-header">
          <h6 class="mb-0">
            <i class="bi bi-person-plus me-2"></i>
            Add Representative
          </h6>
        </div>
        <div class="card-body">
          <!-- Search Input -->
          <div class="mb-3">
            <label for="userSearch" class="form-label">Search Users by Name</label>
            <div class="position-relative">
              <input
                id="userSearch"
                v-model="userSearchQuery"
                type="text"
                class="form-control"
                placeholder="Type to search users by name..."
                @input="debouncedSearchUsers"
              />
              <i class="bi bi-search position-absolute top-50 end-0 translate-middle-y me-3 text-muted"></i>
            </div>
            <div class="form-text">Search for users and click "Add" to make them a representative</div>
          </div>

          <!-- Loading State for Search -->
          <div v-if="searchingUsers" class="text-center py-3">
            <div class="spinner-border spinner-border-sm text-primary me-2"></div>
            <span class="text-muted">Searching users...</span>
          </div>

          <!-- Search Results -->
          <div v-if="searchResults.length > 0 && !searchingUsers" class="mb-3">
            <label class="form-label">Search Results</label>
            <div class="search-results-container" style="max-height: 250px; overflow-y: auto;">
              <div
                v-for="user in searchResults"
                :key="user.id"
                class="user-search-item border rounded p-3 mb-2"
              >
                <div class="d-flex align-items-center">
                  <div class="me-3">
                    <i class="bi bi-person-circle text-primary" style="font-size: 2rem;"></i>
                  </div>
                  <div class="flex-grow-1">
                    <div class="fw-medium">{{ user.name || 'Unknown User' }}</div>
                    <div class="text-muted small">{{ user.email }}</div>
                    <div class="text-muted small">ID: {{ user.id }} | Subject: {{ user.subjectId }}</div>
                  </div>
                  <div class="ms-3">
                    <button
                      type="button"
                      class="btn btn-primary btn-sm"
                      @click="handleAddRepresentative(user)"
                      :disabled="addingRepresentativeIds.has(user.id) || isUserAlreadyRepresentative(user.id)"
                    >
                      <span v-if="addingRepresentativeIds.has(user.id)" class="spinner-border spinner-border-sm me-1"></span>
                      <i v-else-if="isUserAlreadyRepresentative(user.id)" class="bi bi-check-circle me-1"></i>
                      <i v-else class="bi bi-person-plus me-1"></i>
                      {{ addingRepresentativeIds.has(user.id) ? 'Adding...' :
                         isUserAlreadyRepresentative(user.id) ? 'Already Added' : 'Add' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- No Results Message -->
          <div v-if="userSearchQuery && searchResults.length === 0 && !searchingUsers" class="text-muted text-center py-3">
            <i class="bi bi-person-x me-2"></i>
            No users found matching "{{ userSearchQuery }}"
          </div>

          <!-- Error Display -->
          <div v-if="addRepresentativeError" class="alert alert-danger mt-3">
            {{ addRepresentativeError }}
          </div>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading representatives...</span>
      </div>
      <p class="text-muted mt-3">Loading representatives...</p>
    </div>

    <!-- Representatives List -->
    <div v-else-if="representatives?.length" class="representatives-list">
      <div class="d-flex align-items-center justify-content-between mb-3">
        <h6 class="mb-0">Current Representatives</h6>
        <span class="badge bg-info">{{ representatives.length }} {{ representatives.length === 1 ? 'representative' : 'representatives' }}</span>
      </div>
      <div class="row g-3">
        <div
          v-for="representative in representatives"
          :key="representative.id"
          class="col-12"
        >
          <div class="representative-card border rounded p-3">
            <div class="d-flex align-items-start">
              <div class="representative-avatar me-3">
                <i class="bi bi-person-circle text-primary" style="font-size: 2.5rem;"></i>
              </div>
              <div class="flex-grow-1">
                <div class="d-flex align-items-center justify-content-between mb-2">
                  <h6 class="mb-0 fw-medium">
                    {{ representative.name || 'Unknown Representative' }}
                  </h6>
                  <div class="d-flex align-items-center gap-2">
                    <span class="badge bg-success">Active</span>
                    <button
                      class="btn btn-outline-danger btn-sm"
                      @click="handleRemoveRepresentative(representative)"
                      :disabled="removingRepresentatives.has(representative.id)"
                      title="Remove representative"
                    >
                      <span v-if="removingRepresentatives.has(representative.id)" class="spinner-border spinner-border-sm"></span>
                      <i v-else class="bi bi-person-dash"></i>
                    </button>
                  </div>
                </div>
                <div class="representative-details">
                  <div v-if="representative.email" class="mb-1">
                    <i class="bi bi-envelope me-2 text-muted"></i>
                    <a :href="`mailto:${representative.email}`" class="text-decoration-none">
                      {{ representative.email }}
                    </a>
                  </div>
                  <div v-if="representative.organization" class="mb-1">
                    <i class="bi bi-building me-2 text-muted"></i>
                    <span class="text-muted">{{ representative.organization }}</span>
                  </div>
                  <div v-if="representative.subjectId" class="mb-1">
                    <i class="bi bi-person-badge me-2 text-muted"></i>
                    <small class="text-muted">ID: {{ representative.subjectId }}</small>
                  </div>
                  <div v-if="representative.lastLogin" class="mb-1">
                    <i class="bi bi-clock me-2 text-muted"></i>
                    <small class="text-muted">
                      Last login: {{ formatDate(representative.lastLogin) }}
                    </small>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <div class="text-center py-3">
        <i class="bi bi-people text-muted" style="font-size: 2.5rem;"></i>
        <h6 class="text-muted mt-3">No Representatives Assigned</h6>
        <p class="text-muted mb-0">
          Use the search box above to find and add representatives for this resource.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref } from 'vue'
import { useResourcesStore } from '@/store/resources.js'
import { useAdminStore } from '@/store/admin.js'

const props = defineProps({
  representatives: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  resourceId: {
    type: Number,
    required: true,
  },
})

const emit = defineEmits(['representatives-updated'])

const resourcesStore = useResourcesStore()
const adminStore = useAdminStore()

const userSearchQuery = ref('')
const searchResults = ref([])
const addRepresentativeError = ref(null)
const addingRepresentativeIds = ref(new Set())
const removingRepresentatives = ref(new Set())
const searchingUsers = ref(false)

// Debounce utility function
const debounce = (func, wait) => {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

const formatDate = (dateString) => {
  if (!dateString) return 'Never'
  return new Date(dateString).toLocaleDateString()
}

const handleAddRepresentative = async (user) => {
  if (!user.id || !props.resourceId) return

  addRepresentativeError.value = null
  addingRepresentativeIds.value.add(user.id)

  try {
    await resourcesStore.addRepresentativeToResource(
      user.id,
      props.resourceId
    )

    // Refresh representatives list
    emit('representatives-updated')
  } catch (error) {
    console.error('Error adding representative:', error)
    addRepresentativeError.value = 'Failed to add representative. Please try again.'
  } finally {
    addingRepresentativeIds.value.delete(user.id)
  }
}

const handleRemoveRepresentative = async (representative) => {
  if (!representative.id || !props.resourceId) return

  // Confirm removal
  if (!confirm(`Are you sure you want to remove ${representative.name || 'this representative'} from this resource?`)) {
    return
  }

  removingRepresentatives.value.add(representative.id)

  try {
    await resourcesStore.removeRepresentativeFromResource(
      representative.id,
      props.resourceId
    )

    // Refresh representatives list
    emit('representatives-updated')
  } catch (error) {
    console.error('Error removing representative:', error)
  } finally {
    removingRepresentatives.value.delete(representative.id)
  }
}

const searchUsers = async () => {
  if (!userSearchQuery.value.trim()) {
    searchResults.value = []
    return
  }

  searchingUsers.value = true

  try {
    const filtersSortData = {
      name: userSearchQuery.value.trim(),
      email: '',
      subjectId: '',
      isAdmin: '',
      lastLoginAfter: '',
      lastLoginBefore: '',
      sortBy: 'name',
      sortOrder: 'ASC',
    }

    const result = await adminStore.retrieveUsers(0, 20, filtersSortData)
    searchResults.value = result.users || []
  } catch (error) {
    console.error('Error searching users:', error)
    searchResults.value = []
  } finally {
    searchingUsers.value = false
  }
}

const debouncedSearchUsers = debounce(searchUsers, 300)

const isUserAlreadyRepresentative = (userId) => {
  return props.representatives.some(rep => rep.id === userId)
}
</script>

<style scoped>
.representatives-viewer {
  min-height: 300px;
}

.representatives-list {
  max-height: 400px;
  overflow-y: auto;
}

.representative-card {
  background: #ffffff;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.representative-card:hover {
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
  border-color: #0d6efd !important;
}

.representative-avatar {
  flex-shrink: 0;
}

.representative-details {
  font-size: 0.9rem;
}

.representative-details a {
  color: #0d6efd;
}

.representative-details a:hover {
  color: #0a58ca;
}

.empty-state {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  border-radius: 0.5rem;
}

.modal-header {
  border-bottom: 1px solid #dee2e6;
}

.modal-footer {
  border-top: 1px solid #dee2e6;
}

.user-search-item {
  transition: background-color 0.2s ease;
}

.user-search-item:hover {
  background-color: rgba(13, 110, 253, 0.1);
}

.card {
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
}

.card-header {
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
}

.card-body {
  padding: 1.5rem;
}

@media (max-width: 768px) {
  .representatives-viewer {
    padding: 1rem !important;
  }

  .representative-card {
    padding: 1rem !important;
  }
}
</style>
