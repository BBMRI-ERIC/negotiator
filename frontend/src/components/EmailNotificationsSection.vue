<template>
  <div class="email-notifications-section">
    <h3 class="mb-4">Emails</h3>

    <!-- Filters -->
    <AdminSettingsFilterSort
      v-model:filtersSortData="filtersSortData"
      :sort-by-fields="sortByFields"
      :filters-fields="filtersFields"
      :user-role="ROLES.ADMINISTRATOR"
      @filters-sort-data="applyFilters"
    />

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
            <td>{{ formatTimestamp(email.sentAt) }}</td>
            <td>
              <div class="message-preview">
                {{ getMessagePreview(email.message) }}
              </div>
            </td>
            <td>
              <button class="btn btn-sm btn-primary" @click.stop="viewEmailDetails(email)">
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
        <span class="page-info"
          >Page {{ pagination.number + 1 }} of {{ pagination.totalPages }}</span
        >
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
import { ref, onMounted } from 'vue'
import { useEmailStore } from '@/store/emails.js'
import { ROLES } from '@/config/consts'
import AdminSettingsFilterSort from './AdminSettingsFilterSort.vue'
import { formatTimestamp } from '@/composables/utils.js'


const emit = defineEmits(['view-email'])

const emailStore = useEmailStore()

const emails = ref([])
const loading = ref(false)

const sortByFields = ref({
  defaultField: 'address',
  defaultOrder: 'DESC',
  fields: [
    { value: 'address', label: 'Email Address' },
    { value: 'sentAt', label: 'Sent At' }
  ]}
)

const filtersFields = ref([
  { name: 'address', label: 'Email Address', type: 'text', default: '', placeholder: "Enter email address"},
  { name: 'sentAt', label: 'Sent At', type: 'date-range', inputType: 'datetime-local', default: { start: '', end: '' } }
])

const filtersSortData = ref({
  address: '',
  sentAt: {
    start: '', 
    end: ''
  },
  sortBy: sortByFields.value.defaultField,
  sortOrder: sortByFields.value.defaultOrder
})

const pagination = ref({
  number: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
})

const fetchEmails = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.value.number,
      size: pagination.value.size,
      sort:`${filtersSortData.value.sortBy},${filtersSortData.value.sortOrder}`,
    }

    if (filtersSortData.value.address) {
      params.address = filtersSortData.value.address
    }
    if (filtersSortData.value.sentAt.start) {
      // Convert to LocalDateTime format: YYYY-MM-DDTHH:MM:SS
      const afterDate = new Date(filtersSortData.value.sentAt.start)
      params.sentAfter = formatForJavaLocalDateTime(afterDate)
    }
    if (filtersSortData.value.sentAt.end) {
      // Convert to LocalDateTime format: YYYY-MM-DDTHH:MM:SS
      const beforeDate = new Date(filtersSortData.value.sentAt.end)
      params.sentBefore = formatForJavaLocalDateTime(beforeDate)
    }

    const response = await emailStore.fetchNotificationEmails(params)

    emails.value = response._embedded?.notificationEmails || []
    pagination.value = {
      number: response.page.number,
      size: response.page.size,
      totalElements: response.page.totalElements,
      totalPages: response.page.totalPages,
    }
  } catch (error) {
    console.error('Error fetching emails:', error)
    emails.value = []
  } finally {
    loading.value = false
  }
}

const sortBy = (field) => {
  if (filtersSortData.value.sortBy === field) {
    filtersSortData.value.sortOrder = filtersSortData.value.sortOrder === 'ASC' ? 'DESC' : 'ASC'
  } else {
    filtersSortData.value.sortBy = field
    filtersSortData.value.sortOrder = 'ASC'
  }
  pagination.value.number = 0
  fetchEmails()
}

const getSortIcon = (field) => {
  if (filtersSortData.value.sortBy !== field) {
    return 'fas fa-sort text-muted'
  }
  return filtersSortData.value.sortOrder === 'ASC' ? 'fas fa-sort-up text-dark' : 'fas fa-sort-down text-dark'
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

const getMessagePreview = (message) => {
  if (!message) return ''
  const parser = new DOMParser()
  const doc = parser.parseFromString(message, 'text/html')
  const plainText = doc.body.textContent || ''
  return plainText.length > 100 ? plainText.substring(0, 100) + '...' : plainText
}

const viewEmailDetails = (email) => {
  emit('view-email', email)
}

// Helper function to format current datetime for input field
const getCurrentDateTimeForInput = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day}T${hours}:${minutes}`
}

// Helper function to format yesterday at 00:00 for input field
const getYesterdayStartForInput = () => {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  yesterday.setHours(0, 0, 0, 0)

  const year = yesterday.getFullYear()
  const month = String(yesterday.getMonth() + 1).padStart(2, '0')
  const day = String(yesterday.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}T00:00`
}

// Helper function to format date for Java LocalDateTime
const formatForJavaLocalDateTime = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

onMounted(() => {
  // Set sentAfter to yesterday at 00:00 by default
  filtersSortData.value.sentAt.start = getYesterdayStartForInput()
  // Set sentBefore to current date/time by default
  filtersSortData.value.sentAt.end = getCurrentDateTimeForInput()
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
