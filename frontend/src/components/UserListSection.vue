<template>
  <div class="user-list-section">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2 class="text-left">Users</h2>
    </div>
    <div v-if="users.length === 0 && !isLoading" class="text-muted mb-3">No users found.</div>
    <div v-else class="table-container">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Email</th>
            <th>Subject ID</th>
            <th>Last Login</th>
            <th>Admin</th>
            <th>Service Account</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in paginatedUsers" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.name }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.subjectId }}</td>
            <td>{{ formatDate(user.lastLogin) }}</td>
            <td>{{ user.admin ? 'Yes' : 'No' }}</td>
            <td>
              {{ user.serviceAccount === true || user.serviceAccount === 'true' ? 'Yes' : 'No' }}
            </td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <button
          @click="previousPage"
          :disabled="currentPage === 1 || isLoading"
          class="page-button"
        >
          ‹ Prev
        </button>
        <span class="page-info">Page {{ currentPage }} of {{ totalPages }}</span>
        <button
          @click="nextPage"
          :disabled="currentPage === totalPages || isLoading"
          class="page-button"
        >
          Next ›
        </button>
        <input
          v-model.number="pageSize"
          @change="resetPage"
          type="number"
          min="1"
          class="page-size-input"
          placeholder="Page size"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAdminStore } from '@/store/admin.js'

const users = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const totalUsers = ref(0)
const isLoading = ref(false)
const adminStore = useAdminStore()

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

const totalPages = computed(() => {
  return Math.ceil(totalUsers.value / pageSize.value) || 1
})

const paginatedUsers = computed(() => {
  return users.value
})

const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value -= 1
    fetchUsers()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value += 1
    fetchUsers()
  }
}

const resetPage = () => {
  if (pageSize.value < 1) {
    pageSize.value = 10 // Default to 10 if invalid
  }
  currentPage.value = 1
  fetchUsers()
}

const fetchUsers = async () => {
  isLoading.value = true
  try {
    const { users: usersData, totalUsers: totalCount } = await adminStore.retrieveUsers(
      currentPage.value - 1,
      pageSize.value,
    )
    users.value = usersData || []
    totalUsers.value = totalCount || 0
    if (currentPage.value > totalPages.value && totalPages.value > 0) {
      currentPage.value = totalPages.value
      fetchUsers() // Re-fetch if page is out of bounds
    }
  } catch {
    users.value = []
    totalUsers.value = 0
  } finally {
    isLoading.value = false
  }
}

watch(pageSize, () => {
  resetPage()
})

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-list-section {
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

@media (max-width: 768px) {
  .table thead th,
  .table tbody td {
    font-size: 0.85rem;
    padding: 0.75rem;
  }

  .user-list-section {
    padding: 1rem;
  }
}

.table tbody td,
.table thead th {
  /* Prevent overflowing text from breaking the layout */
  word-break: break-word;
  overflow-wrap: anywhere;
  max-width: 200px; /* Adjust as needed for your layout */
  white-space: normal;
}
</style>
