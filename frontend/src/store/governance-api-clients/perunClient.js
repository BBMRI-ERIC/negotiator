import axios from 'axios'
import { perunApiPaths, getBearerHeaders } from '../../config/apiPaths'
import { NegotiatorClient } from './negotiatorClient'

export class PerunClient {
  VIRTUAL_ORGANIZATION_ID = import.meta.env.VITE_PERUN_VO_ID
  BASE_URL = import.meta.env.VITE_PERUN_API_URL
  BIOBANK_ID_ATTR = import.meta.env.VITE_PERUN_BIOBANK_ID_ATTR
  COLLECTION_ID_ATTR = import.meta.env.VITE_PERUN_COLLECTION_ID_ATTR
  GROUP_ATTR_DEF = import.meta.env.VITE_PERUN_GROUP_ATTR_DEF

  negotiatorClient = new NegotiatorClient()

  organizationCache = []

  getOrganizationIdFromGroup(group) {
    const attribute = group.attributes.find(
      (attr) => attr.baseFriendlyName === this.BIOBANK_ID_ATTR,
    )
    return attribute && attribute.value ? attribute.value.trim() : undefined
  }

  getResourceIdFromGroup(group) {
    const attribute = group.attributes.find(
      (attr) => attr.baseFriendlyName === this.COLLECTION_ID_ATTR,
    )
    return attribute ? attribute.value.trim() : undefined
  }

  getUserEmail(user) {
    const attribute = user.userAttributes.find((attr) => attr.baseFriendlyName === 'preferredMail')
    return attribute ? attribute.value.trim() : undefined
  }

  filterOrganizationGroups(groups) {
    return groups.data.filter((group) => {
      return this.getOrganizationIdFromGroup(group) != undefined
    })
  }

  async getAllOrganizations() {
    if (this.organizationCache.length == 0) {
      const params = {
        vo: this.VIRTUAL_ORGANIZATION_ID,
        attrNames: [`${this.GROUP_ATTR_DEF}${this.BIOBANK_ID_ATTR}`],
      }
      const perunGroups = await axios.get(perunApiPaths.GET_GROUPS, {
        params: params,
        headers: getBearerHeaders(),
      })
      const allOrgsGroups = this.filterOrganizationGroups(perunGroups)
      const orgsFromNegotiator = await Promise.all(
        allOrgsGroups.map(async (group) => {
          const orgId = this.getOrganizationIdFromGroup(group)
          if (orgId) {
            const org = await this.negotiatorClient.getOrganizationByExternalId(orgId)
            org.id = group.id
            return group
          }
        }),
      )
      this.organizationCache = orgsFromNegotiator
    }
    return this.organizationCache
  }

  async retrieveOrganizationsPaginated(page = 0, size = 20) {
    const orgsInPage = this.getAllOrganizations().slice(page * size, page * size + size)

    const response = {
      data: {
        _embedded: {
          organizations: orgsInPage,
        },
        _links: {},
        page: {
          size: size,
          totalElements: this.organizationCache.length,
          totalPages: Math.ceil(this.organizationCache.length / size),
          number: page,
        },
      },
    }

    return response
  }

  async getOrganizationById(organizationId) {
    const params = {
      group: organizationId,
      attrNames: [`${this.GROUP_ATTR_DEF}${this.COLLECTION_ID_ATTR}`],
    }
    const perunGroups = await axios.get(perunApiPaths.GET_SUBGROUPS, {
      params,
      headers: getBearerHeaders(),
    })
    const resourcesFromNegotiator = await Promise.all(
      perunGroups.data.map(async (group) => {
        const resourceId = this.getResourceIdFromGroup(group)
        if (resourceId) {
          const resource = await this.negotiatorClient.getResourceBySourceId(resourceId)
          resource.id = group.id

          const params = { group: resource.id }
          const members = await axios.get(perunApiPaths.GET_RICH_MEMBERS, {
            params,
            headers: getBearerHeaders(),
          })
          resource.representatives = members.data.map((member) => {
            return {
              id: member.id,
              name: `${member.user.firstName} ${member.user.lastName}`,
              email: this.getUserEmail(member),
            }
          })

          return resource
        }
      }),
    )
    return {
      data: {
        _embedded: {
          resources: resourcesFromNegotiator,
        },
        _links: {},
        page: {
          // TODO: Check whether this is necessary
          // size: 20,
          // totalElements: orgsFromNegotiator.length,
          // totalPages: 1,
          // number: 0,
        },
      },
    }
  }

  async retrieveUsers(page = 0, size = 10, filtersSortData) {
    // add filtersSortData in case they are valued
    const filters = Object.fromEntries(
      // eslint-disable-next-line
      Object.entries(filtersSortData).filter(([_, value]) => value !== ''),
    )

    const data = {
      vo: this.VIRTUAL_ORGANIZATION_ID,
      attrNames: ['urn:perun:user:attribute-def:def:preferredMail'],
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

  async addRepresentativeToResource(userId, resourceId) {
    const data = {
      member: parseInt(userId),
      group: resourceId,
    }
    const response = await axios.post(`${perunApiPaths.ADD_MEMBER_TO_GROUP}`, data, {
      headers: getBearerHeaders(),
    })
    return response
  }

  async removeRepresentativeFromResource(userId, resourceId) {
    const data = {
      member: parseInt(userId),
      group: resourceId,
    }
    const response = await axios.post(`${perunApiPaths.REMOVE_MEMBER_TO_GROUP}`, data, {
      headers: getBearerHeaders(),
    })
    return response
  }
}
