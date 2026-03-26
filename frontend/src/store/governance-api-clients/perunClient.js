import axios from 'axios'
import { perunApiPaths, getBearerHeaders } from '../../config/apiPaths'
import { NegotiatorClient } from './negotiatorClient'
import { PerunGroupsManager } from './perunGroupManager'

export class PerunClient {
  VIRTUAL_ORGANIZATION_ID = import.meta.env.VITE_PERUN_VO_ID
  BASE_URL = import.meta.env.VITE_PERUN_API_URL
  ORGANIZATION_ID_ATTR = import.meta.env.VITE_PERUN_ORGANIZATION_ID_ATTR
  RESOURCE_ID_ATTR = import.meta.env.VITE_PERUN_RESOURCE_ID_ATTR
  ADMIN_ORGANIZATION_ID_ATTR = import.meta.env.VITE_PERUN_ADMIN_ORGANIZATION_ID_ATTR
  ADMIN_RESOURCE_ID_ATTR = import.meta.env.VITE_PERUN_ADMIN_RESOURCE_ID_ATTR
  GROUP_ATTR_DEF = import.meta.env.VITE_PERUN_GROUP_ATTR_DEF
  USER_ATTR_DEF = import.meta.env.VITE_PERUN_USER_ATTR_DEF
  EMAIL_ATTR_ID = import.meta.env.VITE_PERUN_EMAIL_ATTR

  negotiatorClient = new NegotiatorClient()
  perunGroupsManager = PerunGroupsManager(this.negotiatorClient)

  getUserEmail(user) {
    const attribute = user.userAttributes.find(
      (attr) => attr.baseFriendlyName === this.EMAIL_ATTR_ID,
    )
    return attribute ? attribute.value.trim() : null
  }

  filterOrganizationGroups(groups) {
    return groups.data.filter((group) => {
      return this.getOrganizationIdFromGroup(group) != null
    })
  }

  filterOrganizations(items, filters) {
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

  async getGroupsFromPerun() {
    const params = {
      vo: this.VIRTUAL_ORGANIZATION_ID,
      attrNames: [
        `${this.GROUP_ATTR_DEF}${this.ORGANIZATION_ID_ATTR}`,
        `${this.GROUP_ATTR_DEF}${this.RESOURCE_ID_ATTR}`,
        `${this.GROUP_ATTR_DEF}${this.ADMIN_ORGANIZATION_ID_ATTR}`,
        `${this.GROUP_ATTR_DEF}${this.ADMIN_RESOURCE_ID_ATTR}`,
      ],
    }
    return await axios.get(perunApiPaths.GET_GROUPS, {
      params: params,
      headers: getBearerHeaders(),
    })
  }

  async getAllOrganizations() {
    if (!this.perunGroupsManager.isInitialized()) {
      await this.perunGroupsManager.init(await this.getGroupsFromPerun())
    }
    return this.perunGroupsManager.getNegotiatorOrganizations()
  }

  async retrieveOrganizationsPaginated(page = 0, size = 20, filters = {}) {
    const organizations = this.filterOrganizations(await this.getAllOrganizations(), filters)
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

  async getOrganizationById(organizationId) {
    const negotiatorResources = await Promise.all(
      this.perunGroupsManager.getResourcesInOrganization(organizationId).map(async (resource) => {
        const negotiatorResource = await this.negotiatorClient.getResourceBySourceId(
          resource.directoryId,
        )
        negotiatorResource.perunGroupId = resource.id
        negotiatorResource.representatives = await this.getRepresentativesOfResource(resource.id)
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

  getRepresentedResources(userId, filters = {}) {
    return this.negotiatorClient.getRepresentedResources(userId, filters)
  }

  async retrieveUsers(page = 0, size = 10, filtersSortData) {
    // add filtersSortData in case they are valued
    const filters = Object.fromEntries(
      // eslint-disable-next-line
      Object.entries(filtersSortData).filter(([_, value]) => value !== ''),
    )

    const data = {
      vo: this.VIRTUAL_ORGANIZATION_ID,
      attrNames: [`${this.USER_ATTR_DEF}${this.EMAIL_ATTR_ID}`],
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
              email: this.getUserEmail(perunUser),
            }
          }),
        },
      },
    }
  }

  async getRepresentativesOfResource(perunResourceGroupId) {
    const params = { group: perunResourceGroupId }
    const members = await axios.get(perunApiPaths.GET_RICH_MEMBERS, {
      params,
      headers: getBearerHeaders(),
    })
    return members.data.map((member) => {
      return {
        id: member.id,
        name: `${member.user.firstName} ${member.user.lastName}`,
        email: this.getUserEmail(member),
      }
    })
  }

  async addRepresentativeToResource(userId, resource) {
    const managerGroupId = this.perunGroupsManager.getManagerGroupIdFromResource(resource)
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

  async removeRepresentativeFromResource(userId, resource) {
    const managerGroupId = this.perunGroupsManager.getManagerGroupIdFromResource(resource)
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
}
