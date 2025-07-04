import { ref, watch, onWatcherCleanup } from 'vue'
import { defineStore } from 'pinia'
import moment from 'moment'
import { MAX_QUEUE_SIZE_NOTIFICATIONS, TIME_INTERVAL_NOTIFICATIONS } from '../config/consts'

export const useNotificationsStore = defineStore('notifications', () => {
  const notification = ref({})
  const allNotifications = ref([])
  const currentNotifications = ref([])
  const criticalError = ref(false)

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
    setNotification,
    resetCurrentNotifications,
    removeCurrentNotification,
  }
})
