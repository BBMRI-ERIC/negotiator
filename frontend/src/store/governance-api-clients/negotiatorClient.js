import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../../config/apiPaths'

export class NegotiatorClient {
  async getAllOrganizations(filters = null) {
    const params = filters ? new URLSearchParams(filters).toString() : ''
    const url = params
      ? `${apiPaths.BASE_API_PATH}/organizations?${params}`
      : `${apiPaths.BASE_API_PATH}/organizations`

    return axios.get(url, { headers: getBearerHeaders() })
  }

  async retrieveOrganizationsPaginated(page = 0, size = 20, filters = {}) {
    let url = `${apiPaths.BASE_API_PATH}/organizations`
    const params = {
      page: page,
      size: size,
    }

    // Add name filter if provided
    if (filters.name && filters.name.trim()) {
      params.name = filters.name.trim()
    }

    // Add externalId filter if provided
    if (filters.externalId && filters.externalId.trim()) {
      params.externalId.filters.externalId.trim()
    }

    // Add withdrawn filter if provided
    if (typeof filters.withdrawn === 'boolean') {
      params.withdrawn = filters.withdrawn
    }

    return axios.get(url, { params, headers: getBearerHeaders() })
  }

  async getOrganizationByd(organization, expand = null) {
    const url = expand
      ? `${apiPaths.BASE_API_PATH}/organizations/${organization.id}?expand=${expand}`
      : `${apiPaths.BASE_API_PATH}/organizations/${organization.id}`

    return axios
      .get(url, { headers: getBearerHeaders() })
  }

  async getOrganizationByExternalId(externalId) {
    const params = {
      externalId: externalId,
    }
    const url = `${apiPaths.BASE_API_PATH}/organizations`

    const response = await axios.get(url, { params, headers: getBearerHeaders() })
    if (response.data.page.totalElements > 1) {
        console.log("This is a problem")
    }
    return response.data._embedded.organizations[0]
  }
  
  async getResourceByExternalId(externalId) {
    const url = `${apiPaths.BASE_API_PATH}/resources/${externalId}`

    return axios
      .get(url, { headers: getBearerHeaders() })
  }
}
