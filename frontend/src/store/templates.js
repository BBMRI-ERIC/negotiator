import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useTemplates = defineStore('emailTemplates', () => {
  const notifications = useNotificationsStore()

  function retrieveTemplates() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/templates`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting templates data from server', 'danger')
        return null
      })
  }

  function retrieveTemplateByName(templateName) {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/templates/${templateName}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification(
          'Error getting template by name data from server',
          'danger',
        )
        return null
      })
  }

  function updateTemplate(templateName, data) {
    return axios
      .put(`${apiPaths.BASE_API_PATH}/templates/${templateName}`, data, {
        headers: { ...getBearerHeaders(), 'Content-Type': 'text/plain' },
        responseType: 'text',
      })
      .then(() => {
        notifications.setNotification(
          'Template was updated successfully',
          'success',
        )
      })
      .catch(() => {
        notifications.setNotification('Error updating template', 'danger')
        return null
      })
  }

  function templateReset(templateName) {
    return axios
      .post(`${apiPaths.BASE_API_PATH}/templates/${templateName}/operations`, '{ "operation": "RESET" }', {
        headers: { ...getBearerHeaders(), 'Content-Type': 'application/json' },
      })
      .then(() => {
        notifications.setNotification(
          'Email template was successfully reset',
          'success',
        )
      })
      .catch(() => {
        notifications.setNotification('Error restarting template', 'danger')
        return null
      })
  }

  return {
    retrieveTemplates: retrieveTemplates,
    retrieveTemplateByName: retrieveTemplateByName,
    updateTemplate: updateTemplate,
    templateReset: templateReset,
  }
})
