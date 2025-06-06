import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import moment from 'moment'

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

  function resetNotification() {
    notification.value = {}
  }

  watch(notification, () => {
    if (Object.keys(notification.value).length > 0) {
      setTimeout(() => resetNotification(), 5000)
    }
  })

  watch(currentNotifications, () => {
    if (currentNotifications.value.length > 3) {
      currentNotifications.value.length = 3
    }

    const interval = setInterval(() => {
      if (currentNotifications.value.length > 0) {
        currentNotifications.value.pop()
      } else {
        // Clear the interval when the array is empty
        clearInterval(interval);
      }
    }, 5000);
  })

  return { notification, allNotifications, currentNotifications, criticalError, setNotification, resetNotification }
})
