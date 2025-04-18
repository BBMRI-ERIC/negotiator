import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useEmailTemplates = defineStore('emailTemplates', () => {
  const notifications = useNotificationsStore()

  function retrieveEmailTemplates() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/email-templates`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting email templates data from server', 'danger')
        return null
      })
  }

  function retrieveEmailTemplateByName(templateName) {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/email-templates/${templateName}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification(
          'Error getting email template by name data from server',
          'danger',
        )
        return null
      })
  }

  function updateEmailTemplate(templateName, data) {
    return axios
      .post(`${apiPaths.BASE_API_PATH}/email-templates/${templateName}`, data, {
        headers: getBearerHeaders(),
      })
      .then(() => {
        notifications.setNotification(
          'Thank you. Your email template was submitted successfully',
          'success',
        )
      })
      .catch(() => {
        notifications.setNotification('Error updating email template', 'danger')
        return null
      })
  }

  function creatEmailTemplateReset(templateName) {
    return axios
      .post(`${apiPaths.BASE_API_PATH}/email-templates/${templateName}/reset`, {
        headers: getBearerHeaders(),
      })
      .then(() => {
        notifications.setNotification(
          'Thank you. Your email template was Reseted successfully',
          'success',
        )
      })
      .catch(() => {
        notifications.setNotification('Error restarting email template', 'danger')
        return null
      })
  }

  return {
    retrieveEmailTemplates,
    retrieveEmailTemplateByName,
    updateEmailTemplate,
    creatEmailTemplateReset,
  }
})
