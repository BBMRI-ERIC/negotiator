import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useEmailStore = defineStore('emails', () => {
  const notifications = useNotificationsStore()

  const fetchNotificationEmails = async (params = {}) => {
    const queryParams = new URLSearchParams()

    if (params.page !== undefined) queryParams.append('page', params.page)
    if (params.size !== undefined) queryParams.append('size', params.size)
    if (params.sort) queryParams.append('sort', params.sort)
    if (params.address) queryParams.append('address', params.address)
    if (params.sentAfter) queryParams.append('sentAfter', params.sentAfter)
    if (params.sentBefore) queryParams.append('sentBefore', params.sentBefore)

    const url = `${apiPaths.BASE_API_PATH}/emails${queryParams.toString() ? `?${queryParams.toString()}` : ''}`

    try {
      const response = await axios.get(url, { headers: getBearerHeaders() })
      return response.data
    } catch (error) {
      console.error('Error fetching notification emails:', error)
      notifications.setNotification('Error fetching notification emails from server')
      throw error
    }
  }

  const fetchNotificationEmailById = async (id) => {
    try {
      const response = await axios.get(`${apiPaths.BASE_API_PATH}/emails/${id}`, {
        headers: getBearerHeaders(),
      })
      return response.data
    } catch (error) {
      console.error('Error fetching notification email:', error)
      notifications.setNotification('Error fetching notification email from server')
      throw error
    }
  }

  return {
    fetchNotificationEmails,
    fetchNotificationEmailById,
  }
})
