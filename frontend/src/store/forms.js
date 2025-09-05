import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useFormsStore = defineStore('forms', () => {
  const notifications = useNotificationsStore()

  function retrieveAccessFormById(id) {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/access-forms/${id}`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting request data from server', 'danger')
        return null
      })
  }

  function retrieveAllAccessForms() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/access-forms`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data._embedded['access-forms']
      })
      .catch(() => {
        notifications.setNotification('Error getting request data from server', 'danger')
        return null
      })
  }

  function retrieveAllElements() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/elements`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data._embedded['elements']
      })
      .catch(() => {
        notifications.setNotification('Error getting elements data from server', 'danger')
        return null
      })
  }

  function retrieveDynamicAccessFormsValueSetByLink(link) {
    return axios
      .get(`${link}`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting value-sets request data from server', 'danger')
        return null
      })
  }

  async function submitRequiredInformation(data, negotiationId, requirementId) {
    data.attachments = []
    let isAtachmentError = false
    for (const [sectionName, criteriaList] of Object.entries(data.payload)) {
      for (const [criteriaName, criteriaValue] of Object.entries(criteriaList)) {
        if (criteriaValue instanceof File) {
          const formData = new FormData()
          formData.append('file', criteriaValue)
          const uploadFileHeaders = { headers: getBearerHeaders() }

          uploadFileHeaders['Content-type'] = 'multipart/form-data'

          await axios
            .post('/api/v3/attachments', formData, uploadFileHeaders)
            .then((response) => {
              data.payload[sectionName][criteriaName] = response.data
              data.attachments.push(response.data)
              isAtachmentError = false
            })
            .catch((error) => {
              if (error.response) {
                notifications.setNotification(
                  `There was an error saving the attachment, ${error.response.data.detail}`,
                  'danger',
                )
              } else if (error.request) {
                notifications.setNotification(
                  `There was an error saving the attachment, ${error.request.statusText}`,
                  'danger',
                )
              } else {
                notifications.setNotification(
                  `There was an error saving the attachment, ${error.message}`,
                  'danger',
                )
              }
              isAtachmentError = true
              return null
            })
        }
      }
    }
    if (!isAtachmentError) {
      return axios
        .post(`/api/v3/negotiations/${negotiationId}/info-requirements/${requirementId}`, data, {
          headers: getBearerHeaders(),
        })
        .then((response) => {
          notifications.setNotification(
            'Thank you. Your response was successfully submitted. ',
            'success',
          )
          return response.data.id
        })
        .catch(() => {
          notifications.setNotification('There was an error saving the Negotiation', 'danger')
        })
    }
  }

  function retrieveInfoRequirementsById(requirementId) {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/info-requirements/${requirementId}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification(
          'Error getting info-requirements by id request data from server',
          'danger',
        )
        return null
      })
  }

  async function updateInfoSubmissions(infoSubmissionsId, data) {
    data.attachments = []
    let isAtachmentError = false

    for (const [sectionName, criteriaList] of Object.entries(data.payload)) {
      for (const [criteriaName, criteriaValue] of Object.entries(criteriaList)) {
        if (criteriaValue instanceof File) {
          const formData = new FormData()
          formData.append('file', criteriaValue)
          const uploadFileHeaders = { headers: getBearerHeaders() }

          uploadFileHeaders['Content-type'] = 'multipart/form-data'

          await axios
            .post(`${apiPaths.BASE_API_PATH}/attachments`, formData, uploadFileHeaders)
            .then((response) => {
              data.payload[sectionName][criteriaName] = response.data
              data.attachments.push(response.data)
              isAtachmentError = false
            })
            .catch((error) => {
              if (error.response) {
                notifications.setNotification(
                  `There was an error saving the attachment, ${error.response.data.detail}`,
                  'danger',
                )
              } else if (error.request) {
                notifications.setNotification(
                  `There was an error saving the attachment, ${error.request.statusText}`,
                  'danger',
                )
              } else {
                notifications.setNotification(
                  `There was an error saving the attachment, ${error.message}`,
                  'danger',
                )
              }
              isAtachmentError = true
              return null
            })
        }
      }
    }
    if (!isAtachmentError) {
      return axios
        .patch(`${apiPaths.BASE_API_PATH}/info-submissions/${infoSubmissionsId}`, data, {
          headers: getBearerHeaders(),
        })
        .then((response) => {
          return response.data
        })
        .catch(() => {
          notifications.criticalError = true
          notifications.setNotification(
            `Error updating info-submission: ${infoSubmissionsId}`,
            'warning',
          )
          return null
        })
    }
  }

  function updateInfoSubmissionsIsEdit(infoSubmissionsId, editableData) {
    const data = {
      editable: !editableData,
    }

    return axios
      .patch(`${apiPaths.BASE_API_PATH}/info-submissions/${infoSubmissionsId}`, data, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification(
          `Error updating info-submission is edit: ${infoSubmissionsId}`,
          'warning',
        )
        return null
      })
  }

  return {
    retrieveAccessFormById,
    retrieveAllAccessForms,
    retrieveAllElements,
    retrieveDynamicAccessFormsValueSetByLink,
    submitRequiredInformation,
    retrieveInfoRequirementsById,
    updateInfoSubmissions,
    updateInfoSubmissionsIsEdit,
  }
})
