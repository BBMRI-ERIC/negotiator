<template>
  <div class="dropdown">
    <button
      ref="dropdownButton"
      type="button"
      class="btn btn-sm rounded-circle position-relative py-0 px-1"
      :style="{ color: uiConfiguration?.navbarButtonOutlineColor }"
      data-bs-toggle="dropdown"
      aria-expanded="false"
      @click="() => fetchNotifications(0)"
    >
      <i class="bi bi-bell" />

      <span
        v-if="unreadCount > 0"
        class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger pb-0 px-1"
      >
        {{ unreadCount }}
      </span>
    </button>
    <ul ref="dropdownMenu" class="dropdown-menu dropdown-menu-end">
      <li class="dropdown-item-text alert-info d-flex justify-content-between align-items-center">
        <span>Notifications ({{ totalElements }})</span>
        <button
          v-if="unreadCount > 0"
          @click.stop="markAllAsRead"
          class="btn btn-sm btn-outline-primary"
          title="Mark all as read"
        >
          <i class="bi bi-check-all"></i>
        </button>
      </li>
      <li>
        <hr class="dropdown-divider" />
      </li>
      <li v-if="isLoading" class="dropdown-item text-center">
        <div class="spinner-border spinner-border-sm" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </li>
      <li v-else-if="userNotifications.length === 0" class="dropdown-item text-muted text-center">
        No notifications
      </li>
      <li
        v-else
        v-for="notification in userNotifications"
        :key="notification.id"
        class="dropdown-item py-0"
      >
        <div
          class="alert mb-1 d-flex py-2 mb-2 position-relative"
          :class="notification.read ? 'alert-light' : 'alert-warning'"
          role="alert"
          @click.stop="handleNotificationClick(notification)"
          :style="{ cursor: notification.negotiationId ? 'pointer' : 'default' }"
        >
          <div class="me-auto">
            <div class="fw-bold">{{ notification.title }}</div>
            <div class="small">{{ notification.message }}</div>
            <div v-if="notification.negotiationId" class="small text-primary mt-1">
              <i class="bi bi-arrow-right-circle"></i> Click to view negotiation
            </div>
          </div>
          <div class="ms-3 text-muted small">
            <div>{{ notification.formattedCreatedAt }}</div>
          </div>
        </div>
      </li>

      <!-- Pagination Controls -->
      <li v-if="totalPages > 1" class="dropdown-item">
        <div class="d-flex justify-content-between align-items-center mt-2">
          <button
            @click.stop="changePage(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn btn-sm btn-outline-secondary"
            title="Previous page"
          >
            <i class="bi bi-chevron-left"></i>
          </button>

          <small class="text-muted"> Page {{ currentPage + 1 }} of {{ totalPages }} </small>

          <button
            @click.stop="changePage(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn btn-sm btn-outline-secondary"
            title="Next page"
          >
            <i class="bi bi-chevron-right"></i>
          </button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import { useNotificationsStore } from '../store/notifications.js'
import { useApiCallsStore } from '../store/apiCalls.js'
import { useUserStore } from '../store/user.js'

const router = useRouter()
const uiConfigurationStore = useUiConfiguration()
const notificationsStore = useNotificationsStore()
const apiCallsStore = useApiCallsStore()
const userStore = useUserStore()

// Polling interval (1 minute)
const POLLING_INTERVAL = 1 * 60 * 1000
let pollingIntervalId = null
const isPollingActive = ref(false)

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.navbar
})

const userNotifications = computed(() => {
  return notificationsStore.userNotifications
})

const unreadCount = computed(() => {
  return notificationsStore.unreadCount
})

const isLoading = computed(() => {
  return notificationsStore.isLoading
})

const totalPages = computed(() => {
  return notificationsStore.totalPages
})

const totalElements = computed(() => {
  return notificationsStore.totalElements
})

const currentPage = computed(() => {
  return notificationsStore.currentPage
})

const dropdownButton = ref(null)
const dropdownMenu = ref(null)

async function fetchNotifications(page = 0) {
  if (!userStore.userInfo?.id) {
    await userStore.retrieveUser()
  }

  if (userStore.userInfo?.id) {
    console.log('Fetching notifications for user:', userStore.userInfo.id, 'page:', page)
    notificationsStore.isLoading = true
    try {
      const response = await apiCallsStore.fetchUserNotifications(userStore.userInfo.id, page)
      console.log('Notifications response:', response)
      if (response) {
        notificationsStore.setUserNotifications(response)
      }
    } catch (error) {
      console.error('Error fetching notifications:', error)
    } finally {
      notificationsStore.isLoading = false
    }
  } else {
    console.error('No user ID available for fetching notifications')
  }
}

async function changePage(newPage) {
  if (newPage >= 0 && newPage < totalPages.value) {
    notificationsStore.setCurrentPage(newPage)
    await fetchNotifications(newPage)
  }
}

function startPolling() {
  if (pollingIntervalId) return // Already polling

  isPollingActive.value = true
  pollingIntervalId = setInterval(async () => {
    // Only fetch if user is authenticated and has an ID
    if (userStore.userInfo?.id) {
      try {
        // Always poll the first page to get the latest notifications
        const response = await apiCallsStore.fetchUserNotifications(userStore.userInfo.id, 0)
        if (response) {
          // If we're on page 0, update normally. Otherwise, just update pagination info
          if (currentPage.value === 0) {
            notificationsStore.setUserNotifications(response)
          } else {
            // Update pagination metadata but keep current page content
            if (response.page) {
              notificationsStore.totalPages = response.page.totalPages
              notificationsStore.totalElements = response.page.totalElements
            }
          }
        }
      } catch (error) {
        console.error('Error during periodic notification fetch:', error)
      }
    }
  }, POLLING_INTERVAL)
}

function stopPolling() {
  if (pollingIntervalId) {
    clearInterval(pollingIntervalId)
    pollingIntervalId = null
    isPollingActive.value = false
  }
}

// Handle page visibility changes to pause/resume polling when tab is not active
function handleVisibilityChange() {
  if (document.hidden) {
    stopPolling()
  } else if (userStore.userInfo?.id) {
    startPolling()
    // Fetch fresh data when tab becomes visible again
    fetchNotifications()
  }
}

async function handleNotificationClick(notification) {
  if (notification.negotiationId) {
    // Mark as read and navigate to negotiation
    await markAsReadAndNavigate(notification)
  }
  // If no negotiationId, do nothing (user can use the mark as read button)
}

async function markAsReadAndNavigate(notification) {
  if (!userStore.userInfo?.id) return

  // Mark as read first
  notificationsStore.markNotificationAsRead(notification.id)

  // Send update to backend
  const updates = [{ id: notification.id, read: true }]
  await apiCallsStore.updateNotifications(userStore.userInfo.id, updates)

  // Close dropdown using simple approach
  closeDropdown()

  // Navigate to negotiation
  router.push(`/negotiations/${notification.negotiationId}`)
}
async function markAllAsRead() {
  if (!userStore.userInfo?.id) return

  const unreadNotifications = userNotifications.value.filter((notif) => !notif.read)
  if (unreadNotifications.length === 0) return

  // Optimistically update the UI
  notificationsStore.markAllNotificationsAsRead()

  // Send updates to backend
  const updates = unreadNotifications.map((notif) => ({ id: notif.id, read: true }))
  await apiCallsStore.updateNotifications(userStore.userInfo.id, updates)

  // Close dropdown
  closeDropdown()
}

function closeDropdown() {
  // Simple approach: programmatically trigger a click outside
  if (dropdownButton.value) {
    // Remove the dropdown-toggle state
    dropdownButton.value.blur()
    // Dispatch a click event outside to close the dropdown
    document.body.click()
  }
}

// Initialize component
onMounted(async () => {
  await fetchNotifications()
  startPolling()

  // Listen for page visibility changes
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

// Cleanup
onUnmounted(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.dropdown-menu {
  overflow: hidden;
  overflow-y: auto;
  max-height: calc(100vh - 150px);
}
</style>
