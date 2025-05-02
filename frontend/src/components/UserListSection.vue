<template>
  <div class="d-flex justify-content-between align-items-center mb-1">
    <h2 class="text-left">Users</h2>
  </div>
  <div class="user-list-section">
    <div v-if="users.length === 0" class="text-muted mb-3">
      No users found.
    </div>
    <div v-else class="table-container">
      <table class="table table-hover">
        <thead>
        <tr>
          <th @click="sort('name')" :class="getSortClass('name')">Name</th>
          <th @click="sort('email')" :class="getSortClass('email')">Email</th>
          <th @click="sort('lastLogin')" :class="getSortClass('lastLogin')">Last Login</th>
          <th @click="sort('admin')" :class="getSortClass('admin')">Admin</th>
          <th @click="sort('networkManager')" :class="getSortClass('networkManager')">Network Manager</th>
          <th @click="sort('representativeOfAnyResource')" :class="getSortClass('representativeOfAnyResource')">Resource
            Representative
          </th>
          <th @click="sort('isServiceAccount')" :class="getSortClass('isServiceAccount')">Service Account</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="user in paginatedUsers" :key="user.id">
          <td>{{ user.name }}</td>
          <td>{{ user.email }}</td>
          <td>{{ formatDate(user.lastLogin) }}</td>
          <td>{{ user.admin ? 'Yes' : 'No' }}</td>
          <td>{{ user.networkManager ? 'Yes' : 'No' }}</td>
          <td>{{ user.representativeOfAnyResource ? 'Yes' : 'No' }}</td>
          <td>{{ user.serviceAccount === true || user.serviceAccount === 'true' ? 'Yes' : 'No' }}</td>
        </tr>
        </tbody>
      </table>
      <div class="pagination">
        <button @click="previousPage" :disabled="currentPage === 1" class="page-button">&#8249; Prev</button>
        <span class="page-info">Page {{ currentPage }} of {{ totalPages }}</span>
        <button @click="nextPage" :disabled="currentPage === totalPages" class="page-button">Next &#8250;</button>
        <select v-model="pageSize" @change="resetPage" class="page-size-select">
          <option v-for="size in [10, 25, 50, 100]" :key="size" :value="size">{{ size }} per page</option>
        </select>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, defineProps, ref } from 'vue'

const props = defineProps({
  users: {
    type: Array,
    required: true,
    default: () => []
  }
})

const sortBy = ref('name')
const sortOrder = ref('asc')
const currentPage = ref(1)
const pageSize = ref(10)

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

const sort = (column) => {
  if (sortBy.value === column) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = column
    sortOrder.value = 'asc'
  }
}

const getSortClass = (column) => {
  return column === sortBy.value ? (sortOrder.value === 'asc' ? 'sorted-asc' : 'sorted-desc') : ''
}

const sortedUsers = computed(() => {
  return [...props.users].sort((a, b) => {
    const aValue = a[sortBy.value]
    const bValue = b[sortBy.value]
    if (aValue < bValue) return sortOrder.value === 'asc' ? -1 : 1
    if (aValue > bValue) return sortOrder.value === 'asc' ? 1 : -1
    return 0
  })
})

const totalPages = computed(() => {
  return Math.ceil(sortedUsers.value.length / pageSize.value)
})

const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return sortedUsers.value.slice(start, end)
})

const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value -= 1
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value += 1
  }
}

const resetPage = () => {
  currentPage.value = 1
}
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
  color: #6c757d; /* gray text */
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

.sorted-asc::after {
  content: " ↑";
  font-size: 0.85rem;
}

.sorted-desc::after {
  content: " ↓";
  font-size: 0.85rem;
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

.page-size-select {
  padding: 0.3rem 0.75rem;
  font-size: 0.95rem;
  margin-left: 1rem;
  border: 1px solid #e8ecef;
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
</style>
