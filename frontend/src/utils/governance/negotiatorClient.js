import axios from 'axios'
import { computed } from 'vue'
import { apiPaths, getBearerHeaders } from '../../config/apiPaths'
import { useUserStore } from '@/store/user'

export function NegotiatorClient() {
  const userStore = useUserStore()

  const isManager = computed(() => userStore.isGovernanceManager)

  const getAllOrganizations = async (filters = null) => {
    const params = filters ? new URLSearchParams(filters).toString() : ''
    const url = params
      ? `${apiPaths.BASE_API_PATH}/organizations?${params}`
      : `${apiPaths.BASE_API_PATH}/organizations`

    return axios.get(url, { headers: getBearerHeaders() })
  }

  const retrieveOrganizationsPaginated = async (page = 0, size = 20, filters = {}) => {
    const url = `${apiPaths.BASE_API_PATH}/organizations`

    const params = {
      page: page,
      size: size,
    }

    if (filters.name?.trim()) {
      params.name = filters.name.trim()
    }

    if (filters.externalId?.trim()) {
      params.externalId = filters.externalId.trim()
    }

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
    const params = {
      ...filtersSortData,
      page,
      size,
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

  // eslint-disable-next-line no-unused-vars
  const getRepresentedResources = (userId, page = 0, size = 20, filters = {}) => {
    const url = `${apiPaths.BASE_API_PATH}/users/${userId}/organizations?expand=resources`

    const params = {}
    if (filters.name?.trim()) {
      params.name = filters.name.trim()
    }

    if (typeof filters.withdrawn === 'boolean') {
      params.withdrawn = filters.withdrawn
    }

    return axios.get(url, {
      params,
      headers: getBearerHeaders(),
    })
  }

  const getOrganizationByExternalId = async (externalId) => {
    //FIXME: this is a workoaround to avoid the backend issue
    //described https://github.com/BBMRI-ERIC/negotiator/issues/1429

    if (!externalId) {
      return null
    }
    let page = 0
    let totalPages
    do {
      const response = await axios.get(`${apiPaths.BASE_API_PATH}/organizations`, {
        params: { externalId, page },
        headers: getBearerHeaders(),
      })
      const match = (response.data._embedded?.organizations ?? []).find(
        (organization) => organization.externalId === externalId,
      )
      if (match) {
        return match
      }
      totalPages = response.data.page.totalPages
      page += 1
    } while (page < totalPages)
    return null
  }

  const getResourceBySourceId = async (sourceId) => {
    //FIXME: this is a workoaround to avoid the backend issue
    //described https://github.com/BBMRI-ERIC/negotiator/issues/1430

    if (!sourceId) {
      return null
    }

    let page = 0
    let totalPages
    do {
      const response = await axios.get(`${apiPaths.BASE_API_PATH}/resources`, {
        params: { sourceId, page },
        headers: getBearerHeaders(),
      })
      const match = (response.data._embedded?.resources ?? []).find(
        (resource) => resource.sourceId === sourceId,
      )
      if (match) {
        return match
      }
      totalPages = response.data.page.totalPages
      page += 1
    } while (page < totalPages)
    return null
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
