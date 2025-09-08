import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useNegotiationFormStore = defineStore('negotiationForm', () => {
  const notifications = useNotificationsStore()

  async function retrieveRequestById(requestId) {
    return await axios
      .get(`${apiPaths.REQUESTS_PATH}/${requestId}`, { headers: getBearerHeaders(requestId) })
      .then((response) => {
        // it handles the error when backend is unreachable but vite proxy strangely return 200
        if (response.data === '') {
          return { code: 500 }
        } else {
          return response.data
        }
      })
      .catch((error) => {
        notifications.setNotification('Error getting requestById request data from server')
        if (error.response) {
          return error.response.data
        }
      })
  }

  async function retrieveCombinedAccessForm(requestId) {
    return await axios
      .get(`${apiPaths.BASE_API_PATH}/requests/${requestId}/access-form`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting Combined Access Form request data from server')
        return null
      })
  }

  async function retrieveNegotiationCombinedAccessForm(requestId) {
    return await axios
      .get(`${apiPaths.BASE_API_PATH}/negotiations/${requestId}/access-form`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification(
          'Error getting Negotiation Combined Access Form request data from server',
        )
        return null
      })
  }

  async function retrieveDynamicAccessFormsValueSetByID(id) {
    return await axios
      .get(`${apiPaths.VALUE_SETS}/${id}`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting value-sets request data from server')
        return null
      })
  }

  async function createNegotiation(data) {
    data.attachments = []
    for (const [sectionName, criteriaList] of Object.entries(data.payload)) {
      for (const [criteriaName, criteriaValue] of Object.entries(criteriaList)) {
        if (criteriaValue instanceof File) {
          const formData = new FormData()
          formData.append('file', criteriaValue)
          const uploadFileHeaders = { headers: getBearerHeaders() }

          uploadFileHeaders['Content-type'] = 'multipart/form-data'

          const attachmentsIds = await axios
            .post('/api/v3/attachments', formData, uploadFileHeaders)
            .then((response) => {
              return response.data
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
              return null
            })
          data.payload[sectionName][criteriaName] = attachmentsIds
          data.attachments.push(attachmentsIds)
        }
      }
    }
    return axios
      .post(apiPaths.NEGOTIATION_PATH, data, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data.id
      })
      .catch(() => {
        notifications.setNotification('There was an error saving the Negotiation')
      })
  }

  async function updateNegotiationById(negotiationId, data, disableAutomaticUpload) {
    data.attachments = []
    if (!disableAutomaticUpload) {
      for (const [sectionName, criteriaList] of Object.entries(data.payload)) {
        for (const [criteriaName, criteriaValue] of Object.entries(criteriaList)) {
          if (criteriaValue instanceof File) {
            const formData = new FormData()
            formData.append('file', criteriaValue)
            const uploadFileHeaders = { headers: getBearerHeaders() }

            uploadFileHeaders['Content-type'] = 'multipart/form-data'

            const attachmentsIds = await axios
              .post(
                `${apiPaths.BASE_API_PATH}/negotiations/${negotiationId}/attachments`,
                formData,
                uploadFileHeaders,
              )
              .then((response) => {
                return response.data
              })
              .catch((error) => {
                if (error.response) {
                  notifications.setNotification(
                    `There was an error updating the attachment, ${error.response.data.detail}`,
                    'danger',
                  )
                } else if (error.request) {
                  notifications.setNotification(
                    `There was an error updating the attachment, ${error.request.statusText}`,
                    'danger',
                  )
                } else {
                  notifications.setNotification(
                    `There was an error updating the attachment, ${error.message}`,
                    'danger',
                  )
                }
                return null
              })
            data.payload[sectionName][criteriaName] = attachmentsIds
            data.attachments.push(attachmentsIds)
          }
        }
      }
    }
    return axios
      .patch(`${apiPaths.NEGOTIATION_PATH}/${negotiationId}`, data, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.criticalError = true
        notifications.setNotification(`Error updating Negotiation: ${negotiationId}`, 'warning')
        return null
      })
  }

  function deleteAttachment(attachmentsId) {
    return axios
      .delete(`${apiPaths.BASE_API_PATH}/attachments/${attachmentsId}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response
      })
      .catch(() => {
        notifications.setNotification(`Error deleting attachment: ${attachmentsId}`, 'warning')
        return null
      })
  }

  async function transferNegotiation(negotiationId, subjectId) {
    const response = await axios.patch(
      `${apiPaths.NEGOTIATION_PATH}/${negotiationId}`,
      { authorSubjectId: subjectId },
      { headers: getBearerHeaders() },
    )
    return response.data
  }

  async function retrieveFormElements() {
    return await axios
      .get(`${apiPaths.BASE_API_PATH}/elements`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data._embedded.elements
      })
      .catch(() => {
        notifications.setNotification('Error getting Form elements data from server')
        return null
      })
  }

  async function addAccessForm(data) {
    return await axios
      .post(`${apiPaths.BASE_API_PATH}/access-forms`, data, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error adding access form', 'warning')
      })
  }

  async function addAccessFormSections(data) {
    return await axios
      .post(`${apiPaths.BASE_API_PATH}/sections`, data, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error adding access form')
      })
  }

  async function updateAccessFormSections(id, data) {
    return await axios
      .put(`${apiPaths.BASE_API_PATH}/sections/${id}`, data, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error updating access form', 'warning')
      })
  }

  async function linkSectionToAccessForm(formId, data) {
    return await axios
      .put(`${apiPaths.BASE_API_PATH}/access-forms/${formId}/sections`, data, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error linking section to access form')
      })
  }

  function unlinkSectionFromAccessForm(formId, sectionId) {
    return axios
      .delete(`${apiPaths.BASE_API_PATH}/access-forms/${formId}/sections/${sectionId}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error un-linking section from access form')
      })
  }

  async function linkElementsToSectionToAccessForm(formId, sectionId, data) {
    return await axios
      .put(
        `${apiPaths.BASE_API_PATH}/access-forms/${formId}/sections/${sectionId}/elements`,
        data,
        {
          headers: getBearerHeaders(),
        },
      )
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error linking elemets to specific section in an access form')
      })
  }

  async function deleteLinkElementFromSectionInAccessForm(formId, sectionId, elementId) {
    return await axios
      .delete(
        `${apiPaths.BASE_API_PATH}/access-forms/${formId}/sections/${sectionId}/elements/${elementId}`,
        {
          headers: getBearerHeaders(),
        },
      )
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification(
          'Error deleting elemet link from specific section in an access form',
        )
      })
  }

  async function retrieveAccessForms() {
    return await axios
      .get(`${apiPaths.BASE_API_PATH}/access-forms`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data._embedded
      })
      .catch(() => {
        notifications.setNotification('Error getting Form elements data from server')
        return null
      })
  }

  async function retrieveAccessFormById(id) {
    return await axios
      .get(`${apiPaths.BASE_API_PATH}/access-forms/${id}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting access form data from server')
        return null
      })
  }

  return {
    retrieveRequestById,
    retrieveCombinedAccessForm,
    retrieveNegotiationCombinedAccessForm,
    retrieveDynamicAccessFormsValueSetByID,
    createNegotiation,
    updateNegotiationById,
    deleteAttachment,
    transferNegotiation,
    retrieveFormElements,
    addAccessForm,
    addAccessFormSections,
    updateAccessFormSections,
    linkSectionToAccessForm,
    unlinkSectionFromAccessForm,
    linkElementsToSectionToAccessForm,
    deleteLinkElementFromSectionInAccessForm,
    retrieveAccessForms,
    retrieveAccessFormById,
  }
})
