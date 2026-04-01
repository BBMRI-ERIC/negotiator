import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../../config/apiPaths'
import { useUserStore } from '@/store/user'
import { ROLES } from '@/config/consts.js'

export function NegotiatorClient() {
  const userStore = useUserStore()

  const isManager = () => {
    return userStore.userInfo.roles.includes(ROLES.ADMINISTRATOR)
  }

  const getAllOrganizations = async (filters = null) => {
    const params = filters ? new URLSearchParams(filters).toString() : ''
    const url = params
      ? `${apiPaths.BASE_API_PATH}/organizations?${params}`
      : `${apiPaths.BASE_API_PATH}/organizations`

    return axios.get(url, { headers: getBearerHeaders() })
  }

  const retrieveOrganizationsPaginated = async (page = 0, size = 20, filters = {}) => {
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

  const getOrganizationById = (organizationId, expand = null) => {
    const url = expand
      ? `${apiPaths.BASE_API_PATH}/organizations/${organizationId}?expand=${expand}`
      : `${apiPaths.BASE_API_PATH}/organizations/${organizationId}`

    return axios.get(url, { headers: getBearerHeaders() })
  }

  const retrieveUsers = (filtersSortData, page = 0, size = 10) => {
    // add filtersSortData in case they are valued
    const params = {
      ...filtersSortData,
      page,
      size
    }
    return axios.get(`${apiPaths.BASE_API_PATH}/users`, {
      headers: getBearerHeaders(),
      params: params,
    })
  }

  const addRepresentativeToResource = (userId, resource) => {
    return axios.patch(
      `${apiPaths.BASE_API_PATH}/users/${userId}/resources`,
      { id: resource.id },
      { headers: getBearerHeaders() },
    )
  }

  const removeRepresentativeFromResource = (userId, resource) => {
    return axios.delete(`${apiPaths.BASE_API_PATH}/users/${userId}/resources/${resource.id}`, {
      headers: getBearerHeaders(),
    })
  }

  const getRepresentedResources = (userId, filters = {}) => {
    let url = `${apiPaths.BASE_API_PATH}/users/${userId}/organizations?expand=resources`

    if (filters.name && filters.name.trim()) {
      url += `&name=${encodeURIComponent(filters.name.trim())}`
    }

    if (typeof filters.withdrawn === 'boolean') {
      url += `&withdrawn=${filters.withdrawn}`
    }

    return axios.get(url, {
      headers: getBearerHeaders(),
    })
  }

  const getOrganizationByExternalId = async (externalId) => {
    const params = {
      externalId: externalId,
    }
    const response = await axios.get(`${apiPaths.BASE_API_PATH}/organizations`, {
      params,
      headers: getBearerHeaders(),
    })
    return response.data.page.totalElements == 1 ? response.data._embedded.organizations[0] : null
  }

  const getResourceBySourceId = async (sourceId) => {
    const params = {
      sourceId: sourceId,
    }
    const response = await axios.get(`${apiPaths.BASE_API_PATH}/resources`, {
      params,
      headers: getBearerHeaders(),
    })
    return response.data.page.totalElements == 1 ? response.data._embedded.resources[0] : null
  }

  return {
    isManager,
    getAllOrganizations,
    getOrganizationById,
    retrieveOrganizationsPaginated,
    retrieveUsers,
    addRepresentativeToResource,
    removeRepresentativeFromResource,
    getRepresentedResources,
    getOrganizationByExternalId,
    getResourceBySourceId,
  }
}
