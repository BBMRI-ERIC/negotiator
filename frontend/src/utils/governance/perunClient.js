import axios from 'axios'
import { perunApiPaths, getBearerHeaders } from '../../config/apiPaths'
import { NegotiatorClient } from './negotiatorClient'
import { PerunGroupsManager, NegotiatorOrganization, NegotiatorResource } from './groupsManager'
import governanceSettings from '@/config/governance'
import { useUserStore } from '@/store/user'
import { ROLES } from '@/config/consts.js'

export function PerunClient() {
  const VIRTUAL_ORGANIZATION_ID = governanceSettings.virtualOrganizationId
  const ORGANIZATION_ID_ATTR = governanceSettings.organizationIdAttr
  const RESOURCE_ID_ATTR = governanceSettings.resourceIdAttr
  const ADMIN_ORGANIZATION_ID_ATTR = governanceSettings.adminOrganizationIdAttr
  const ADMIN_RESOURCE_ID_ATTR = governanceSettings.adminResourceIdAttr
  const GROUP_ATTR_DEF = governanceSettings.groupAttrDef
  const USER_ATTR_DEF = governanceSettings.userAttrDef
  const EMAIL_ATTR_ID = governanceSettings.emailAttrId
  const REPRESENTATIVE_ACTIONS = {
    ADD: 'A',
    REMOVE: 'R',
  }

  const negotiatorClient = new NegotiatorClient()

  const perunGroupsManager = PerunGroupsManager(negotiatorClient)

  const userStore = useUserStore()

  const isManager = () => {
    return (
      userStore.userInfo.roles.includes(ROLES.ADMINISTRATOR) ||
      userStore.userInfo.roles.includes(ROLES.NETWORK_MANAGER)
    )
  }

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
      perunGroupsManager.init(await getGroupsFromPerun())

      const orgReprGroups = perunGroupsManager.getOrganizationsRepresentativesGroups()
      await Promise.all(
        orgReprGroups.map(async (orgReprGroup) => {
          const negotiatorOrgData = await negotiatorClient.getOrganizationByExternalId(
            orgReprGroup.getNegotiatorId(),
          )
          if (negotiatorOrgData) {
            const negotiatorOrg = NegotiatorOrganization(orgReprGroup, negotiatorOrgData)
            orgReprGroup.setNegotiatorOrganization(negotiatorOrg)
          }
        }),
      )
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
      perunGroupsManager
        .getResourcesRepresentativesGroupsForOrganization(organizationId)
        .map(async (resourceGroup) => {
          const negotiatorData = await negotiatorClient.getResourceBySourceId(
            resourceGroup.getNegotiatorId(),
          )
          const representatives = await getRepresentativesOfResource(resourceGroup.getPerunId())
          return NegotiatorResource(resourceGroup, negotiatorData, representatives)
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

  const getRepresentedResources = async (userId, page, size, filters = {}) => {
    const organizations = await retrieveOrganizationsPaginated(page, size, filters)
    await Promise.all(
      organizations.data._embedded.organizations.map(async (organization) => {
        organization.resources = (
          await getOrganizationById(organization.id)
        ).data._embedded.resources
      }),
    )
    return organizations
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

  const addOrRemoveRepresentativeToResource = async (userId, resource, action) => {
    const url =
      action == REPRESENTATIVE_ACTIONS.ADD
        ? perunApiPaths.ADD_MEMBER_TO_GROUP
        : perunApiPaths.REMOVE_MEMBER_TO_GROUP

    const resourceGroupId = resource.getResourceGroupId()

    const data = {
      member: parseInt(userId),
      group: resourceGroupId,
    }
    await axios.post(url, data, {
      headers: getBearerHeaders(),
    })

    return { data: null }
  }

  const addRepresentativeToResource = async (userId, resource) => {
    return await addOrRemoveRepresentativeToResource(userId, resource, REPRESENTATIVE_ACTIONS.ADD)
  }

  const removeRepresentativeFromResource = async (userId, resource) => {
    return await addOrRemoveRepresentativeToResource(
      userId,
      resource,
      REPRESENTATIVE_ACTIONS.REMOVE,
    )
  }

  return {
    isManager,
    getAllOrganizations,
    retrieveOrganizationsPaginated,
    getOrganizationById,
    retrieveUsers,
    getRepresentedResources,
    addRepresentativeToResource,
    removeRepresentativeFromResource,
  }
}
