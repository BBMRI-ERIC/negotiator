import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useResourcesStore = defineStore('resources', () => {
  const notifications = useNotificationsStore()

  function getResourceById(id) {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/resources/${id}`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error fetching resource details from server')
        throw error
      })
  }

  function getAllResources(filters = null) {
    const params = filters ? new URLSearchParams(filters).toString() : ''
    const url = params
      ? `${apiPaths.BASE_API_PATH}/resources?${params}`
      : `${apiPaths.BASE_API_PATH}/resources`

    return axios
      .get(url, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error fetching resources from server')
        throw error
      })
  }

  function createResources(resourcesData) {
    return axios
      .post(`${apiPaths.BASE_API_PATH}/resources`, resourcesData, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error creating resources')
        throw error
      })
  }

  function updateResource(id, resourceData) {
    return axios
      .patch(`${apiPaths.BASE_API_PATH}/resources/${id}`, resourceData, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error updating resource')
        throw error
      })
  }

  return {
    getResourceById,
    getAllResources,
    createResources,
    updateResource,
  }
})
