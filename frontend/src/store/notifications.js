import { ref, watch } from 'vue'
import { defineStore } from 'pinia'

export const useNotificationsStore = defineStore('notifications', () => {
  const notification = ref({})
  const allNotifications = ref([])
  const criticalError = ref(false)

  function setNotification(notificationMessage, notificationType) {
    const notificationObject = {
      message: notificationMessage,
      type: notificationType,
    }
    notification.value = notificationObject
    if (Object.keys(notificationObject).length > 0) {
      allNotifications.value.push(notificationObject)
    }
  }

  function resetNotification() {
    notification.value = {}
  }

  watch(notification, () => {
    if (notification.value) {
      setTimeout(() => resetNotification(), 5000)
    }
  })

  return { notification, allNotifications, criticalError, setNotification, resetNotification }
})
