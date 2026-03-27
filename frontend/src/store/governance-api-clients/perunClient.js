import axios from 'axios'
import { perunApiPaths, getBearerHeaders } from '../../config/apiPaths'
import { NegotiatorClient } from './negotiatorClient'
import { PerunGroupsManager } from './perunGroupManager'

export function PerunClient() {
  const VIRTUAL_ORGANIZATION_ID = import.meta.env.VITE_PERUN_VO_ID
  const ORGANIZATION_ID_ATTR = import.meta.env.VITE_PERUN_ORGANIZATION_ID_ATTR
  const RESOURCE_ID_ATTR = import.meta.env.VITE_PERUN_RESOURCE_ID_ATTR
  const ADMIN_ORGANIZATION_ID_ATTR = import.meta.env.VITE_PERUN_ADMIN_ORGANIZATION_ID_ATTR
  const ADMIN_RESOURCE_ID_ATTR = import.meta.env.VITE_PERUN_ADMIN_RESOURCE_ID_ATTR
  const GROUP_ATTR_DEF = import.meta.env.VITE_PERUN_GROUP_ATTR_DEF
  const USER_ATTR_DEF = import.meta.env.VITE_PERUN_USER_ATTR_DEF
  const EMAIL_ATTR_ID = import.meta.env.VITE_PERUN_EMAIL_ATTR

  const negotiatorClient = new NegotiatorClient()
  const perunGroupsManager = PerunGroupsManager(negotiatorClient)

  const getUserEmail = (user) => {
    const attribute = user.userAttributes.find((attr) => attr.baseFriendlyName === EMAIL_ATTR_ID)
    return attribute ? attribute.value.trim() : null
  }

  const filterOrganizations = (items, filters) => {
    if (Object.keys(filters).length == 0) return items

    return items.filter((item) => {
      const nameMatch =
        filters.name === undefined ||
        item.name.toLowerCase().includes(filters.name.toLowerCase()) ||
        item.externalId.toLowerCase().includes(filters.name.toLowerCase())

      const withdrawnMatch = filters.withdrawn === undefined || item.withdrawn === filters.withdrawn

      return nameMatch && withdrawnMatch
    })
  }

  const getGroupsFromPerun = async () => {
    const params = {
      vo: VIRTUAL_ORGANIZATION_ID,
      attrNames: [
        `${GROUP_ATTR_DEF}${ORGANIZATION_ID_ATTR}`,
        `${GROUP_ATTR_DEF}${RESOURCE_ID_ATTR}`,
        `${GROUP_ATTR_DEF}${ADMIN_ORGANIZATION_ID_ATTR}`,
        `${GROUP_ATTR_DEF}${ADMIN_RESOURCE_ID_ATTR}`,
      ],
    }
    return await axios.get(perunApiPaths.GET_GROUPS, {
      params: params,
      headers: getBearerHeaders(),
    })
  }

  const getAllOrganizations = async () => {
    if (!perunGroupsManager.isInitialized()) {
      await perunGroupsManager.init(await getGroupsFromPerun())
    }
    return perunGroupsManager.getNegotiatorOrganizations()
  }

  const retrieveOrganizationsPaginated = async (page = 0, size = 20, filters = {}) => {
    const organizations = filterOrganizations(await getAllOrganizations(), filters)
    return {
      data: {
        _embedded: {
          organizations: organizations.slice(page * size, page * size + size),
        },
        _links: {},
        page: {
          size: size,
          totalElements: organizations.length,
          totalPages: Math.ceil(organizations.length / size),
          number: page,
        },
      },
    }
  }

  const getOrganizationById = async (organizationId) => {
    const negotiatorResources = await Promise.all(
      perunGroupsManager.getResourcesInOrganization(organizationId).map(async (resource) => {
        const negotiatorResource = await negotiatorClient.getResourceBySourceId(
          resource.directoryId,
        )
        negotiatorResource.perunGroupId = resource.id
        negotiatorResource.representatives = await getRepresentativesOfResource(resource.id)
        return negotiatorResource
      }),
    )
    return {
      data: {
        _embedded: {
          resources: negotiatorResources,
        },
        _links: {},
        page: {},
      },
    }
  }

  const getRepresentedResources = (userId, filters = {}) => {
    return negotiatorClient.getRepresentedResources(userId, filters)
  }

  const retrieveUsers = async (page = 0, size = 10, filtersSortData) => {
    // add filtersSortData in case they are valued
    const filters = Object.fromEntries(
      // eslint-disable-next-line
      Object.entries(filtersSortData).filter(([_, value]) => value !== ''),
    )

    const data = {
      vo: VIRTUAL_ORGANIZATION_ID,
      attrNames: [`${USER_ATTR_DEF}${EMAIL_ATTR_ID}`],
      query: {
        pageSize: 5,
        offset: page * size,
        order: 'ASCENDING',
        sortColumn: 'NAME',
        searchString: filters.name,
        statuses: [],
        groupId: null,
      },
    }

    const response = await axios.post(`${perunApiPaths.GET_MEMBERS}`, data, {
      headers: getBearerHeaders(),
    })

    return {
      data: {
        page: {
          size: response.data.pageSize,
          totalElements: response.data.totalCount,
          totalPages: Math.ceil(response.data.totalCount / response.data.pageSize),
          number: response.data.offset,
        },
        _embedded: {
          users: response.data.data.map((perunUser) => {
            return {
              id: `${perunUser.id}`,
              name: `${perunUser.user.firstName} ${perunUser.user.lastName}`,
              email: getUserEmail(perunUser),
            }
          }),
        },
      },
    }
  }

  const getRepresentativesOfResource = async (perunResourceGroupId) => {
    const params = { group: perunResourceGroupId }
    const members = await axios.get(perunApiPaths.GET_RICH_MEMBERS, {
      params,
      headers: getBearerHeaders(),
    })
    return members.data.map((member) => {
      return {
        id: member.id,
        name: `${member.user.firstName} ${member.user.lastName}`,
        email: getUserEmail(member),
      }
    })
  }

  const addRepresentativeToResource = async (userId, resource) => {
    const managerGroupId = perunGroupsManager.getManagerGroupIdFromResource(resource)
    for (const groupId of [resource.perunGroupId, managerGroupId]) {
      const data = {
        member: parseInt(userId),
        group: groupId,
      }
      await axios.post(`${perunApiPaths.ADD_MEMBER_TO_GROUP}`, data, {
        headers: getBearerHeaders(),
      })
    }
    return { data: null }
  }

  const removeRepresentativeFromResource = async (userId, resource) => {
    const managerGroupId = perunGroupsManager.getManagerGroupIdFromResource(resource)
    for (const groupId of [resource.perunGroupId, managerGroupId]) {
      const data = {
        member: parseInt(userId),
        group: groupId,
      }
      await axios.post(`${perunApiPaths.REMOVE_MEMBER_TO_GROUP}`, data, {
        headers: getBearerHeaders(),
      })
    }
    return { data: null }
  }

  return {
    getAllOrganizations,
    retrieveOrganizationsPaginated,
    getOrganizationById,
    retrieveUsers,
    getRepresentedResources,
    addRepresentativeToResource,
    removeRepresentativeFromResource,
  }
}
