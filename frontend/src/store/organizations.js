import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useOrganizationsStore = defineStore('organizations', () => {
  const notifications = useNotificationsStore()

  function getOrganizationById(id, expand = null) {
    const url = expand
      ? `${apiPaths.BASE_API_PATH}/organizations/${id}?expand=${expand}`
      : `${apiPaths.BASE_API_PATH}/organizations/${id}`

    return axios
      .get(url, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error fetching organization details from server')
        throw error
      })
  }

  function updateOrganization(id, organizationData) {
    return axios
      .patch(`${apiPaths.BASE_API_PATH}/organizations/${id}`, organizationData, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error updating organization')
        throw error
      })
  }

  function getAllOrganizations(filters = null) {
    const params = filters ? new URLSearchParams(filters).toString() : ''
    const url = params
      ? `${apiPaths.BASE_API_PATH}/organizations?${params}`
      : `${apiPaths.BASE_API_PATH}/organizations`

    return axios
      .get(url, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error fetching organizations from server')
        throw error
      })
  }

  function createOrganizations(organizationsData) {
    return axios
      .post(`${apiPaths.BASE_API_PATH}/organizations`, organizationsData, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error creating organizations')
        throw error
      })
  }

  return {
    getOrganizationById,
    updateOrganization,
    getAllOrganizations,
    createOrganizations,
  }
})
