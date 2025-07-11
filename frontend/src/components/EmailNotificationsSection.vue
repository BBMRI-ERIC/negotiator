<template>
  <div class="email-notifications-section">
    <h3 class="mb-4">Email Notifications</h3>

    <!-- Filters -->
    <div class="row mb-3">
      <div class="col-md-4">
        <label for="addressFilter" class="form-label">Filter by Email Address</label>
        <input
          id="addressFilter"
          v-model="filters.address"
          type="text"
          class="form-control"
          placeholder="Enter email address..."
          @input="debouncedFilter"
        />
      </div>
      <div class="col-md-3">
        <label for="sentAfter" class="form-label">Sent After</label>
        <input
          id="sentAfter"
          v-model="filters.sentAfter"
          type="datetime-local"
          class="form-control"
          @change="applyFilters"
        />
      </div>
      <div class="col-md-3">
        <label for="sentBefore" class="form-label">Sent Before</label>
        <input
          id="sentBefore"
          v-model="filters.sentBefore"
          type="datetime-local"
          class="form-control"
          @change="applyFilters"
        />
      </div>
      <div class="col-md-2 d-flex align-items-end">
        <button
          class="btn btn-outline-secondary w-100"
          @click="clearFilters"
        >
          Clear Filters
        </button>
      </div>
    </div>

    <!-- Loading indicator -->
    <div v-if="loading" class="text-center py-4">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- Email table -->
    <div v-else-if="emails.length > 0" class="table-container">
      <table class="table table-hover">
        <thead>
          <tr>
            <th scope="col">
              <button
                class="btn btn-link p-0 text-decoration-none text-dark"
                @click="sortBy('address')"
              >
                Email Address
                <i :class="getSortIcon('address')"></i>
              </button>
            </th>
            <th scope="col">
              <button
                class="btn btn-link p-0 text-decoration-none text-dark"
                @click="sortBy('sentAt')"
              >
                Sent At
                <i :class="getSortIcon('sentAt')"></i>
              </button>
            </th>
            <th scope="col">Message Preview</th>
            <th scope="col">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="email in emails"
            :key="email.id"
            class="cursor-pointer"
            @click="viewEmailDetails(email)"
          >
            <td>{{ email.address }}</td>
            <td>{{ formatDate(email.sentAt) }}</td>
            <td>
              <div class="message-preview">
                {{ getMessagePreview(email.message) }}
              </div>
            </td>
            <td>
              <button
                class="btn btn-sm btn-primary"
                @click.stop="viewEmailDetails(email)"
              >
                View Details
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="pagination">
        <button
          @click="goToPage(pagination.number - 1)"
          :disabled="pagination.number === 0"
          class="page-button"
        >
          ‹ Prev
        </button>
        <span class="page-info">Page {{ pagination.number + 1 }} of {{ pagination.totalPages }}</span>
        <button
          @click="goToPage(pagination.number + 1)"
          :disabled="pagination.number >= pagination.totalPages - 1"
          class="page-button"
        >
          Next ›
        </button>
        <input
          v-model.number="pagination.size"
          @change="changePageSize"
          type="number"
          min="1"
          class="page-size-input"
          placeholder="Page size"
        />
      </div>
    </div>

    <!-- No data message -->
    <div v-else class="text-center py-4">
      <p class="text-muted">No email notifications found.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useEmailStore } from '@/store/emails.js'

const emit = defineEmits(['view-email'])

const emailStore = useEmailStore()

// Simple debounce function to replace lodash
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

const emails = ref([])
const loading = ref(false)
const filters = ref({
  address: '',
  sentAfter: '',
  sentBefore: ''
})

const pagination = ref({
  number: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0
})

const sort = ref({
  field: 'sentAt',
  direction: 'desc'
})

const visiblePages = computed(() => {
  const total = pagination.value.totalPages
  const current = pagination.value.number
  const delta = 2

  const range = []
  const rangeWithDots = []

  for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
    range.push(i)
  }

  if (range[0] > 1) {
    rangeWithDots.push(0)
    if (range[0] > 2) rangeWithDots.push('...')
  } else if (range[0] === 1) {
    rangeWithDots.push(0)
  }

  rangeWithDots.push(...range)

  if (range[range.length - 1] < total - 2) {
    rangeWithDots.push('...')
    rangeWithDots.push(total - 1)
  } else if (range[range.length - 1] === total - 2) {
    rangeWithDots.push(total - 1)
  }

  return rangeWithDots.filter(page => page !== '...')
})

const debouncedFilter = debounce(() => {
  applyFilters()
}, 500)

const fetchEmails = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.value.number,
      size: pagination.value.size,
      sort: `${sort.value.field},${sort.value.direction}`
    }

    if (filters.value.address) {
      params.address = filters.value.address
    }
    if (filters.value.sentAfter) {
      params.sentAfter = new Date(filters.value.sentAfter).toISOString()
    }
    if (filters.value.sentBefore) {
      params.sentBefore = new Date(filters.value.sentBefore).toISOString()
    }

    const response = await emailStore.fetchNotificationEmails(params)

    emails.value = response._embedded?.notificationEmails || []
    pagination.value = {
      number: response.page.number,
      size: response.page.size,
      totalElements: response.page.totalElements,
      totalPages: response.page.totalPages
    }
  } catch (error) {
    console.error('Error fetching emails:', error)
    emails.value = []
  } finally {
    loading.value = false
  }
}

const sortBy = (field) => {
  if (sort.value.field === field) {
    sort.value.direction = sort.value.direction === 'asc' ? 'desc' : 'asc'
  } else {
    sort.value.field = field
    sort.value.direction = 'asc'
  }
  pagination.value.number = 0
  fetchEmails()
}

const getSortIcon = (field) => {
  if (sort.value.field !== field) {
    return 'fas fa-sort text-muted'
  }
  return sort.value.direction === 'asc'
    ? 'fas fa-sort-up text-dark'
    : 'fas fa-sort-down text-dark'
}

const goToPage = (page) => {
  if (page >= 0 && page < pagination.value.totalPages) {
    pagination.value.number = page
    fetchEmails()
  }
}

const changePageSize = () => {
  pagination.value.number = 0
  fetchEmails()
}

const applyFilters = () => {
  pagination.value.number = 0
  fetchEmails()
}

const clearFilters = () => {
  filters.value = {
    address: '',
    sentAfter: '',
    sentBefore: ''
  }
  pagination.value.number = 0
  fetchEmails()
}

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleString()
}

const getMessagePreview = (message) => {
  if (!message) return ''
  const plainText = message.replace(/<[^>]*>/g, '')
  return plainText.length > 100 ? plainText.substring(0, 100) + '...' : plainText
}

const viewEmailDetails = (email) => {
  emit('view-email', email)
}

onMounted(() => {
  fetchEmails()
})
</script>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}

.cursor-pointer:hover {
  background-color: var(--bs-gray-100);
}

.message-preview {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-info {
  min-width: 200px;
}

.btn-link {
  border: none !important;
  text-decoration: none !important;
}

.btn-link:hover {
  text-decoration: underline !important;
}

.table-container {
  margin-top: 1rem;
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
}

.page-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.page-info {
  font-size: 0.95rem;
  color: #6c757d;
}

.page-size-input {
  padding: 0.3rem 0.75rem;
  font-size: 0.95rem;
  margin-left: 1rem;
  width: 60px;
  border: 1px solid #e8ecef;
  text-align: center;
}
</style>
