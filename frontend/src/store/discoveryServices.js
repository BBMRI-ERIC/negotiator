import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useDiscoveryServicesStore = defineStore('discoveryServices', () => {
  const notifications = useNotificationsStore()

  async function retrieveDiscoveryServices() {
    return await axios
      .get(`${apiPaths.DISCOVERY_SERVICES}`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification(
          'Failed to load discovery services. Please try again or contact support if the issue persists.',
          'danger',
        )
        if (error.response) {
          return error.response.data
        }
      })
  }

  return {
    retrieveDiscoveryServices,
  }
})
