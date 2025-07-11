import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'
import { ROLES } from '@/config/consts'

export const useApiCallsStore = defineStore('apiCalls', () => {
  const notifications = useNotificationsStore()

  function createRequests(data) {
    return axios
      .post(`${apiPaths.REQUESTS_PATH}`, data)
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('There was an error saving the attachment')
        return null
      })
  }

  function markMessageAsRead(data) {
    return axios
      .put(`${apiPaths.NEGOTIATION_PATH}/${data.negotiationId}/posts/${data.postId}`, data, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data.id
      })
      .catch(() => {
        return 'Failed'
      })
  }

  function getUnreadMessagesByRole(data) {
    // the role shoud be complementary in relation of the one from the user
    const complementaryRole =
      data.Rolename === ROLES.RESEARCHER ? ROLES.REPRESENTATIVE : ROLES.RESEARCHER
    return axios
      .get(`${apiPaths.NEGOTIATION_PATH}/${data.negotiationId}/${complementaryRole}/posts`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        return 'Failed'
      })
  }

  function fetchUserNotifications(userId, page = 0, size = 10) {
    return axios
      .get(`${apiPaths.USER_NOTIFICATIONS_PATH}/${userId}/notifications`, {
        headers: getBearerHeaders(),
        params: {
          page,
          size
        }
      })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Failed to fetch notifications', 'danger')
        console.error('Error fetching notifications:', error)
        console.error('Request URL:', `${apiPaths.USER_NOTIFICATIONS_PATH}/${userId}/notifications`)
        console.error('Request params:', { page, size})
        return null
      })
  }

  function updateNotifications(userId, updates) {
    return axios
      .patch(`${apiPaths.USER_NOTIFICATIONS_PATH}/${userId}/notifications`, updates, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Failed to update notifications', 'danger')
        console.error('Error updating notifications:', error)
        return null
      })
  }

  return {
    createRequests,
    markMessageAsRead,
    getUnreadMessagesByRole,
    fetchUserNotifications,
    updateNotifications,
  }
})
