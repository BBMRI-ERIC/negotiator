import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'

export const useDiscoveryServicesStore = defineStore('discoveryServices', () => {
  async function fetchDiscoveryServices() {
    try {
      const response = await axios.get(`${apiPaths.BASE_API_PATH}/discovery-services`, {
        headers: getBearerHeaders(),
      })
      return response.data || []
    } catch (error) {
      console.error('Failed to fetch discovery services:', error)
      return []
    }
  }

  return {
    fetchDiscoveryServices,
  }
})

