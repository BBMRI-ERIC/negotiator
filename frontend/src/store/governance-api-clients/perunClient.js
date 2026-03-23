import axios from 'axios'
import { perunApiPaths, getBearerHeaders } from '../../config/apiPaths'
import { NegotiatorClient } from './negotiatorClient'

export class PerunClient {
  VIRTUAL_ORGANIZATION_ID = import.meta.env.VITE_PERUN_VO_ID
  BASE_URL = import.meta.env.VITE_PERUN_API_URL
  BIOBANK_ID_ATTR = import.meta.env.VITE_PERUN_BIOBANK_ID_ATTR
  COLLECTION_ID_ATTR = import.meta.env.VITE_PERUN_COLLECTION_ID_ATTR
  GROUP_ATTR_DEF = import.meta.env.VITE_PERUN_GROUP_ATTR_DEF
  REPRESENTATIVES_GROUP_INDEX = 3
  REPRESENTATIVES_GROUP_NAME = import.meta.env.VITE_PERUN_REPRESENTATIVE_GROUP_NAME

  negotiatorClient = new NegotiatorClient()

  async getAllOrganizations() {
    const url = perunApiPaths.GET_NATIONAL_NODES
    const params = {
      parentGroup: 46015,
    }
    return axios.get(url, { params, headers: getBearerHeaders() }).then((organizations) => {
      return organizations.filter((org) => {
        const parts = org.name.split(':')
        return (
          parts.length >= 3 &&
          parts[parts.length - this.REPRESENTATIVES_GROUP_INDEX] === this.REPRESENTATIVES_GROUP_NAME
        )
      })
    })
  }

  getOrganizationIdFromGroup(group) {
    const attribute = group.attributes.find(
      (attr) => attr.baseFriendlyName === this.BIOBANK_ID_ATTR,
    )
    return attribute ? attribute.value.trim() : undefined
  }

  getResourceIdFromGroup(group) {
    const attribute = group.attributes.find(
      (attr) => attr.baseFriendlyName === this.COLLECTION_ID_ATTR,
    )
    return attribute ? attribute.value.trim() : undefined
  }

  async retrieveOrganizationsPaginated() {
    let url = perunApiPaths.GET_GROUPS
    const params = {
      vo: this.VIRTUAL_ORGANIZATION_ID,
      attrNames: [`${this.GROUP_ATTR_DEF}${this.BIOBANK_ID_ATTR}`],
    }
    const perunGroups = await axios.get(url, { params: params, headers: getBearerHeaders() })
    const orgsGroups = perunGroups.data.filter((org) => {
      const parts = org.name.split(':')
      return (
        parts.length == 3 &&
        parts[parts.length - this.REPRESENTATIVES_GROUP_INDEX] === this.REPRESENTATIVES_GROUP_NAME
      )
    })
    const orgsFromNegotiator = await Promise.all(
      orgsGroups.map(async (group) => {
        const orgId = this.getOrganizationIdFromGroup(group)
        if (orgId) {
          const org = await this.negotiatorClient.getOrganizationByExternalId(orgId)
          return {
            ...org,
            perunGroupId: group.id,
          }
        }
      }),
    )

    return {
      data: {
        _embedded: {
          organizations: orgsFromNegotiator,
        },
        _links: {},
        page: {
          // TODO: create the pagination
          size: 20,
          totalElements: orgsFromNegotiator.length,
          totalPages: 1,
          number: 0,
        },
      },
    }
  }

  async getOrganizationByd(organization) {
    const params = {
      group: organization.perunGroupId,
      attrNames: [`${this.GROUP_ATTR_DEF}${this.COLLECTION_ID_ATTR}`],
    }

    const url = perunApiPaths.GET_SUBGROUPS
    const perunGroups = await axios.get(url, { params, headers: getBearerHeaders() })
    const resourcesFromNegotiator = await Promise.all(
      perunGroups.data.map(async (group) => {
        const resourceId = this.getResourceIdFromGroup(group)
        if (resourceId) {
          const resource = await this.negotiatorClient.getResourceBySourceId(resourceId)

          return {
            ...resource,
            perunGroupId: group.id,
          }
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
}
