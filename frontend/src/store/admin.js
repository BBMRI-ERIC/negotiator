import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'
import { getGovernanceClient } from '../utils/governance'

export const useAdminStore = defineStore('admin', () => {
  const notifications = useNotificationsStore()
  const governanceClient = getGovernanceClient()

  function retrieveResourceAllEvents() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/resource-lifecycle/events`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data._embedded.events
      })
      .catch(() => {
        notifications.setNotification('Error getting all resource events data from server')
      })
  }

  function setInfoRequirements(data) {
    return axios
      .post(`${apiPaths.BASE_API_PATH}/info-requirements`, data, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error sending message')
      })
  }

  function retrieveInfoRequirement(link) {
    // Convert absolute URLs to relative paths for proper proxy handling in dev
    const url = link.startsWith('http') ? new URL(link).pathname : link
    return axios
      .get(url, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting Info Requirements data from server')
      })
  }

  function retrieveInfoRequirements() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/info-requirements`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data._embedded
      })
      .catch(() => {
        notifications.setNotification('Error getting Info Requirements data from server')
      })
  }

  function retrieveWebhooks() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/webhooks`, { headers: getBearerHeaders() })
      .then((response) => {
        if (!response.data._embedded || !response.data._embedded.webhookResponseDTOList) {
          return []
        }
        return response.data._embedded.webhookResponseDTOList
      })
      .catch((error) => {
        notifications.setNotification('Error getting webhooks from server')
        console.error('Error retrieving webhooks:', error)
        return []
      })
  }

  function createWebhook(data) {
    return axios
      .post(`${apiPaths.BASE_API_PATH}/webhooks`, data, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error sending message')
      })
  }

  function updateWebhook(id, data) {
    return axios
      .patch(`${apiPaths.BASE_API_PATH}/webhooks/${id}`, data, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error sending message')
      })
  }

  function deleteWebhook(webhookId) {
    return axios
      .delete(`${apiPaths.BASE_API_PATH}/webhooks/${webhookId}`, {
        headers: getBearerHeaders(),
      })
      .then(() => {
        notifications.setNotification('Webhook deleted successfully')
      })
      .catch((error) => {
        notifications.setNotification('Error deleting webhook', 'danger')
        throw error
      })
  }

  function testWebhook(webhookId) {
    return axios
      .post(
        `${apiPaths.BASE_API_PATH}/webhooks/${webhookId}/deliveries`,
        { test: 'yes' },
        {
          headers: getBearerHeaders(),
        },
      )
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Failed to send test delivery', 'danger')
        throw error
      })
  }

  function getWebhook(webhookId) {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/webhooks/${webhookId}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => response.data)
  }

  function redeliverWebhookDelivery(webhookId, deliveryId) {
    return axios
      .post(
        `${apiPaths.BASE_API_PATH}/webhooks/${webhookId}/deliveries/${deliveryId}/redeliver`,
        null,
        {
          headers: getBearerHeaders(),
        },
      )
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Failed to redeliver webhook delivery', 'danger')
        throw error
      })
  }

  function retrieveUsers(filtersSortData, page = 0, size = 10) {
    const filterSortDataWirhValues = Object.fromEntries(
      Object.entries(filtersSortData).filter(([, value]) => value !== ''),
    )

    return governanceClient
      .retrieveUsers(filterSortDataWirhValues, page, size)
      .then((response) => {
        return {
          users: response.data.page.totalElements > 0 ? response.data._embedded.users : [],
          totalUsers: response.data.page.totalElements, // Total count of users (for pagination)
        }
      })
      .catch(() => {
        notifications.setNotification('Error getting users data from server')
        return { users: [], totalUsers: 0 }
      })
  }

  function retrieveResources() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/resources`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data._embedded?.resources || []
      })
      .catch(() => {
        notifications.setNotification('Error getting resources data from server')
        return []
      })
  }

  function retrieveResourcesPaginated(page = 0, size = 20, filters = {}) {
    let url = `${apiPaths.BASE_API_PATH}/resources?page=${page}&size=${size}`

    // Add name filter if provided
    if (filters.name && filters.name.trim()) {
      url += `&name=${encodeURIComponent(filters.name.trim())}`
    }

    // Add sourceId filter if provided
    if (filters.sourceId && filters.sourceId.trim()) {
      url += `&sourceId=${encodeURIComponent(filters.sourceId.trim())}`
    }

    return axios
      .get(url, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting resources data from server')
        return {
          _embedded: { resources: [] },
          page: { number: 0, totalPages: 0, totalElements: 0 },
          _links: {},
        }
      })
  }

  function fetchResourcesPage(url) {
    return axios
      .get(url, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error fetching resources page')
        return {
          _embedded: { resources: [] },
          page: { number: 0, totalPages: 0, totalElements: 0 },
          _links: {},
        }
      })
  }

  function updateResource(id, data) {
    return axios
      .patch(`${apiPaths.BASE_API_PATH}/resources/${id}`, data, { headers: getBearerHeaders() })
      .then((response) => {
        notifications.setNotification('Resource updated successfully')
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error updating resource', 'danger')
        throw new Error('Failed to update resource')
      })
  }

  function retrieveOrganizationsPaginated(page = 0, size = 20, filters = {}) {
    return governanceClient
      .retrieveOrganizationsPaginated(page, size, filters)
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting organizations data from server')
        return {
          _embedded: { organizations: [] },
          page: { number: 0, totalPages: 0, totalElements: 0 },
          _links: {},
        }
      })
  }

  function updateOrganization(id, data) {
    return axios
      .patch(`${apiPaths.BASE_API_PATH}/organizations/${id}`, data, { headers: getBearerHeaders() })
      .then((response) => {
        notifications.setNotification('Organization updated successfully')
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error updating organization', 'danger')
        throw new Error('Failed to update organization')
      })
  }

  function createOrganization(data) {
    // The API expects an array of organizations, so wrap the single organization in an array
    const organizationsArray = [data]

    return axios
      .post(`${apiPaths.BASE_API_PATH}/organizations`, organizationsArray, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        notifications.setNotification('Organization created successfully')
        // The API returns a collection, so extract the first (and only) organization
        return response.data._embedded?.organizations?.[0] || response.data
      })
      .catch((error) => {
        notifications.setNotification('Error creating organization', 'danger')
        throw error
      })
  }

  return {
    retrieveResourceAllEvents,
    setInfoRequirements,
    retrieveInfoRequirement,
    retrieveInfoRequirements,
    retrieveWebhooks,
    createWebhook,
    updateWebhook,
    deleteWebhook,
    testWebhook,
    getWebhook,
    redeliverWebhookDelivery,
    retrieveUsers,
    retrieveResources,
    retrieveResourcesPaginated,
    fetchResourcesPage,
    updateResource,
    retrieveOrganizationsPaginated,
    updateOrganization,
    createOrganization,
  }
})
