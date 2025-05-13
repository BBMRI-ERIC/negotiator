import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useNegotiationPageStore = defineStore('negotiationPage', () => {
  const notifications = useNotificationsStore()

  function updateNegotiationStatus(negotiationId, event, message) {
    return axios
      .put(
        `${apiPaths.NEGOTIATION_PATH}/${negotiationId}/lifecycle/${event}`,
        { message },
        { headers: getBearerHeaders() },
      )
      .then((response) => {
        notifications.setNotification(`Negotiation updated correctly with data ${response.data.id}`)
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error updating negotiation status')
        return null
      })
  }

  async function retrievePossibleEvents(negotiationId) {
    return axios
      .get(`${apiPaths.NEGOTIATION_PATH}/${negotiationId}/lifecycle`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting Possible Events data from server')
      })
  }

  function retrievePossibleEventsForResource(negotiationId, resourceId) {
    return axios
      .get(`${apiPaths.NEGOTIATION_PATH}/${negotiationId}/resources/${resourceId}/lifecycle`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting Possible Events For Resource data from server')
      })
  }

  function updateResourceStatus(link) {
    return axios
      .put(`${link}`, {}, { headers: getBearerHeaders() })
      .then((response) => {
        notifications.setNotification(
          `Than you. Your action for Negotiation ${response.data.id} was submitted successfully`,
        )
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error updating negotiation status')
        return null
      })
  }

  async function retrieveNegotiationById(negotiationId) {
    return axios
      .get(`${apiPaths.NEGOTIATION_PATH}/${negotiationId}`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.criticalError = true
        notifications.setNotification(
          `Error getting Negotiation: ${negotiationId}, it doesn't exist or you don't have permission to access it.`,
          'warning',
        )
        return null
      })
  }

  async function retrievePostsByNegotiationId(negotiationId) {
    const url = `${apiPaths.NEGOTIATION_PATH}/${negotiationId}/posts`
    return await axios
      .get(url, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting Negotiation Posts data from server')
      })
  }

  async function retrieveAttachmentsByNegotiationId(negotiationId) {
    const url = `${apiPaths.NEGOTIATION_PATH}/${negotiationId}/attachments`
    return await axios
      .get(url, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting Negotiation Attachments data from server')
      })
  }

  async function addMessageToNegotiation(data) {
    return await axios
      .post(`${apiPaths.NEGOTIATION_PATH}/${data.negotiationId}/posts`, data, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error sending message')
      })
  }

  async function addAttachmentToNegotiation(data) {
    const formData = new FormData()
    const uploadFileHeaders = { headers: getBearerHeaders() }
    if (data.organizationId != null) {
      formData.append('organizationId', data.organizationId)
    }
    formData.append('file', data.attachment)
    uploadFileHeaders['Content-type'] = 'multipart/form-data'

    return await axios
      .post(
        `${apiPaths.NEGOTIATION_PATH}/${data.negotiationId}/attachments`,
        formData,
        uploadFileHeaders,
      )
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('There was an error saving the attachment')
        return null
      })
  }

  async function retrieveUserIdRepresentedResources(userId) {
    return await axios
      .get(`${apiPaths.BASE_API_PATH}/users/${userId}/resources`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data._embedded?.resources
      })
      .catch(() => {
        notifications.setNotification('Error getting User Represented Resources data from server')
      })
  }

  function downloadAttachment(id, name) {
    axios
      .get(`${apiPaths.ATTACHMENTS_PATH}/${id}`, {
        headers: getBearerHeaders(),
        responseType: 'blob',
      })
      .then((response) => {
        const href = window.URL.createObjectURL(response.data)

        const anchorElement = document.createElement('a')
        anchorElement.href = href
        anchorElement.download = name

        document.body.appendChild(anchorElement)
        anchorElement.click()

        document.body.removeChild(anchorElement)
        window.URL.revokeObjectURL(href)
      })
  }

  async function retrieveResourcesByNegotiationId(negotiationId) {
    return await axios
      .get(`${apiPaths.NEGOTIATION_PATH}/${negotiationId}/resources`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data?._embedded?.resources
      })
      .catch(() => {
        notifications.setNotification('Error fetching Resources', 'danger')
      })
  }

  async function retrieveResourcesByNegotiationIdLinks(negotiationId) {
    return await axios
      .get(`${apiPaths.NEGOTIATION_PATH}/${negotiationId}/resources`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error fetching Resources', 'danger')
      })
  }

  function downloadAttachmentFromLink(href) {
    axios.get(`${href}`, { headers: getBearerHeaders(), responseType: 'blob' }).then((response) => {
      const disposition = response.headers['Content-Disposition']
      let filename = 'summary.csv'
      console.log(response.headers)
      if (disposition) {
        const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/
        const matches = filenameRegex.exec(disposition)
        if (matches != null && matches[1]) {
          filename = matches[1].replace(/['"]/g, '')
        }
      }
      const href = window.URL.createObjectURL(response.data)

      const anchorElement = document.createElement('a')
      anchorElement.href = href
      anchorElement.download = filename

      document.body.appendChild(anchorElement)
      anchorElement.click()

      document.body.removeChild(anchorElement)
      window.URL.revokeObjectURL(href)
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

  function retrieveInformationSubmission(href) {
    return axios
      .get(`${href}`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('Error getting Information Submission data from server')
      })
  }

  async function retrieveAllResources(name) {
    let url = `${apiPaths.BASE_API_PATH}/resources`
    if (name) {
      url = `${apiPaths.BASE_API_PATH}/resources?name=${name}`
    }
    return axios
      .get(`${url}`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('There was an error saving the attachment')
        return null
      })
  }

  async function fetchURL(url) {
    return axios
      .get(`${url}`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data
      })
      .catch(() => {
        notifications.setNotification('There was an error saving the attachment')
        return null
      })
  }

  async function addResources(data, negotiationId) {
    try {
      const response = await axios.patch(
        `${apiPaths.BASE_API_PATH}/negotiations/${negotiationId}/resources`,
        data,
        { headers: getBearerHeaders() },
      )
      notifications.setNotification('Resources were successfully updated')
      return response.data
    } catch {
      notifications.setNotification('There was an error saving the attachment')
      return undefined
    }
  }

  async function retrieveResourceAllStates() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/resource-lifecycle/states`, { headers: getBearerHeaders() })
      .then((response) => {
        return response.data._embedded.states
      })
      .catch(() => {
        notifications.setNotification('There was an error saving the attachment')
        return null
      })
  }

  async function deleteNegotiation(negotiationId) {
    return axios
      .delete(`${apiPaths.NEGOTIATION_PATH}/${negotiationId}`, { headers: getBearerHeaders() })
      .then(() => {
        notifications.setNotification(`Negotiation with id ${negotiationId} deleted successfully`)
      })
      .catch(() => {
        notifications.setNotification('Error deleting negotiation')
      })
  }

  async function retrieveNegotiationPDF (id) {
    axios
      .get(`${apiPaths.NEGOTIATION_PATH}/${id}/pdf`, {
        headers: getBearerHeaders(),
        responseType: 'blob',
      })
      .then((response) => {
        const href = window.URL.createObjectURL(response.data)

        const anchorElement = document.createElement('a')
        anchorElement.href = href
        anchorElement.download = name

        document.body.appendChild(anchorElement)
        anchorElement.click()

        document.body.removeChild(anchorElement)
        window.URL.revokeObjectURL(href)
      })
  }

  return {
    updateNegotiationStatus,
    retrievePossibleEvents,
    retrievePossibleEventsForResource,
    retrieveInfoRequirement,
    updateResourceStatus,
    retrieveNegotiationById,
    retrievePostsByNegotiationId,
    retrieveAttachmentsByNegotiationId,
    addMessageToNegotiation,
    addAttachmentToNegotiation,
    retrieveUserIdRepresentedResources,
    downloadAttachment,
    retrieveResourcesByNegotiationId,
    retrieveResourcesByNegotiationIdLinks,
    downloadAttachmentFromLink,
    retrieveInformationSubmission,
    fetchURL,
    addResources,
    retrieveAllResources,
    retrieveResourceAllStates,
    deleteNegotiation,
    retrieveNegotiationPDF
  }
})
