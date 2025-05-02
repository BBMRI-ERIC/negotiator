import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useAdminStore = defineStore('admin', () => {
  const notifications = useNotificationsStore()

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
    return axios
      .get(`${link}`, { headers: getBearerHeaders() })
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
        headers: getBearerHeaders()
      })
      .then(() => {
        notifications.setNotification('Webhook deleted successfully')
      })
      .catch((error) => {
        notifications.setNotification('Error deleting webhook', 'error')
        throw error
      })
  }

  function testWebhook(webhookId) {
    return axios
      .post(
        `${apiPaths.BASE_API_PATH}/webhooks/${webhookId}/deliveries`,
        { 'test': 'yes' },
        {
          headers: getBearerHeaders()
        }
      )
      .then(response => {
        return response.data
      })
      .catch(error => {
        notifications.setNotification('Failed to send test delivery', 'error')
        throw error
      });
  }


  function getWebhook(webhookId) {
    return axios.get(`${apiPaths.BASE_API_PATH}/webhooks/${webhookId}`, {
      headers: getBearerHeaders()
    }).then(response => response.data)
  }

  function retrieveUsers() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/users`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data._embedded.users
      })
      .catch(() => {
        notifications.setNotification('Error getting users data from server')
        return []
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
    retrieveUsers
  }
})
