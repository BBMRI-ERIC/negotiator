import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import governanceSettings from '../config/governance'
import { ROLES } from '@/config/consts'
// import { useNotificationsStore } from "./notifications"

export const useUserStore = defineStore('user', () => {
  // const notifications = useNotificationsStore()
  const userInfo = ref({})

  async function retrieveUser() {
    return await axios
      .get(apiPaths.USER_PATH, { headers: getBearerHeaders() })
      .then((response) => {
        userInfo.value = response.data
      })
      .catch(() => {
        // notifications.setNotification("Error sending message")
      })
  }

  const isAdmin = computed(() => {
    return userInfo.value.roles?.includes(ROLES.ADMINISTRATOR)
  })

  const isGovernanceManager = computed(() =>
    governanceSettings.managerRoles.some((role) => userInfo.value.roles?.includes(role)),
  )

  return { userInfo, retrieveUser, isGovernanceManager, isAdmin }
})
