import { ref } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { ROLES } from '@/config/consts'
// import { useNotificationsStore } from "./notifications"

export const useUserStore = defineStore('user', () => {
  // const notifications = useNotificationsStore()
  const userInfo = ref({})

  async function retrieveUser() {
    return await axios
      .get(apiPaths.USER_PATH, { headers: getBearerHeaders() })
      .then((response) => {
        if (response.data.networkManager === true) {
          response.data.roles.push(ROLES.NETWORK_MANAGER)
        }
        userInfo.value = response.data
      })
      .catch(() => {
        // notifications.setNotification("Error sending message")
      })
  }

  return { userInfo, retrieveUser }
})
