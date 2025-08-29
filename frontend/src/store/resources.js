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

  function getResourceWithRepresentatives(id) {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/resources/${id}?expand=representatives`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error fetching resource details with representatives')
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

  function addRepresentativeToResource(userId, resourceId, silent = false) {
    return axios
      .patch(
        `${apiPaths.BASE_API_PATH}/users/${userId}/resources`,
        { id: resourceId },
        { headers: getBearerHeaders() },
      )
      .then((response) => {
        if (!silent) {
          notifications.setNotification('Representative added successfully', 'success')
        }
        return response.data
      })
      .catch((error) => {
        if (!silent) {
          notifications.setNotification('Error adding representative to resource')
        }
        throw error
      })
  }

  function removeRepresentativeFromResource(userId, resourceId, silent = false) {
    return axios
      .delete(`${apiPaths.BASE_API_PATH}/users/${userId}/resources/${resourceId}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        if (!silent) {
          notifications.setNotification('Representative removed successfully', 'success')
        }
        return response.data
      })
      .catch((error) => {
        if (!silent) {
          notifications.setNotification('Error removing representative from resource')
        }
        throw error
      })
  }

  return {
    getResourceById,
    getResourceWithRepresentatives,
    getAllResources,
    createResources,
    updateResource,
    addRepresentativeToResource,
    removeRepresentativeFromResource,
  }
})
