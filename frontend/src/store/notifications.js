import { ref, watch, onWatcherCleanup } from 'vue'
import { defineStore } from 'pinia'
import moment from 'moment'
import { MAX_QUEUE_SIZE_NOTIFICATIONS, TIME_INTERVAL_NOTIFICATIONS } from '../config/consts'

export const useNotificationsStore = defineStore('notifications', () => {
  const notification = ref({})
  const allNotifications = ref([])
  const currentNotifications = ref([])
  const criticalError = ref(false)
  const isLoading = ref(false)
  const userNotifications = ref([])
  const unreadCount = ref(0)
  const currentPage = ref(0)
  const totalPages = ref(0)
  const totalElements = ref(0)
  const pageSize = ref(10)

  // Legacy function for showing temporary notifications
  function setNotification(notificationMessage, notificationType) {
    const notificationObject = {
      message: notificationMessage,
      type: notificationType,
      timestamp: moment().format('HH:mm:ss'),
    }
    notification.value = notificationObject
    if (Object.keys(notificationObject).length > 0) {
      allNotifications.value = [notificationObject, ...allNotifications.value]
      currentNotifications.value = [notificationObject, ...currentNotifications.value]
    }
  }

  // New function to set user notifications from API with pagination info
  function setUserNotifications(notifications) {
    if (notifications && notifications._embedded && notifications._embedded.notifications) {
      userNotifications.value = notifications._embedded.notifications.map((notif) => ({
        ...notif,
        timestamp: moment(notif.createdAt).format('MMM DD, HH:mm'),
        formattedCreatedAt: moment(notif.createdAt).fromNow(),
      }))
      updateUnreadCount()

      // Extract pagination info
      if (notifications.page) {
        currentPage.value = notifications.page.number
        totalPages.value = notifications.page.totalPages
        totalElements.value = notifications.page.totalElements
        pageSize.value = notifications.page.size
      }
    } else {
      userNotifications.value = []
      unreadCount.value = 0
      currentPage.value = 0
      totalPages.value = 0
      totalElements.value = 0
    }
  }

  function setCurrentPage(page) {
    currentPage.value = page
  }

  function updateUnreadCount() {
    unreadCount.value = userNotifications.value.filter((notif) => !notif.read).length
  }

  function markNotificationAsRead(notificationId) {
    const notification = userNotifications.value.find((notif) => notif.id === notificationId)
    if (notification) {
      notification.read = true
      updateUnreadCount()
    }
  }

  function markAllNotificationsAsRead() {
    userNotifications.value.forEach((notif) => {
      notif.read = true
    })
    updateUnreadCount()
  }

  function resetCurrentNotifications() {
    currentNotifications.value = []
  }

  function removeCurrentNotification(index) {
    if (index > -1) {
      currentNotifications.value.splice(index, 1)
    }
  }

  let activeInterval = null // Variable to store the active interval ID

  watch(currentNotifications, () => {
    if (currentNotifications.value.length > 3) {
      currentNotifications.value = currentNotifications.value.slice(0, MAX_QUEUE_SIZE_NOTIFICATIONS)
    }
    // Clear any existing interval before creating a new one
    if (activeInterval !== null) {
      clearInterval(activeInterval)
    }
    activeInterval = setInterval(() => {
      if (currentNotifications.value.length > 0) {
        currentNotifications.value.pop()
      } else {
        // Clear the interval when the array is empty
        clearInterval(activeInterval)
        activeInterval = null // Reset the active interval variable
      }
    }, TIME_INTERVAL_NOTIFICATIONS)
    onWatcherCleanup(() => {
      // Clear the active interval during watcher cleanup
      if (activeInterval !== null) {
        clearInterval(activeInterval)
        activeInterval = null // Reset the active interval variable
      }
    })
  })

  return {
    notification,
    allNotifications,
    currentNotifications,
    criticalError,
    isLoading,
    userNotifications,
    unreadCount,
    currentPage,
    totalPages,
    totalElements,
    pageSize,
    setNotification,
    setUserNotifications,
    setCurrentPage,
    updateUnreadCount,
    markNotificationAsRead,
    markAllNotificationsAsRead,
    resetCurrentNotifications,
    removeCurrentNotification,
  }
})
