import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'
import { getGovernanceClient } from '../utils/governance'

export const useResourcesStore = defineStore('resources', () => {
  const notifications = useNotificationsStore()
  const governanceClient = getGovernanceClient()

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

  function addRepresentativeToResource(userId, resource, silent = false) {
    return governanceClient
      .addRepresentativeToResource(userId, resource)
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

  function removeRepresentativeFromResource(userId, resource, silent = false) {
    return governanceClient
      .removeRepresentativeFromResource(userId, resource)
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

  function getRepresentedResources(userId, silent = false, page = 0, size = 20, filters = {}) {
    console.log(filters)
    return governanceClient
      .getRepresentedResources(userId, page, size, filters)
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        if (!silent) {
          notifications.setNotification('Error retrieving represented resources')
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
    getRepresentedResources,
  }
})
