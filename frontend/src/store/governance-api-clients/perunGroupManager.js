export function PerunGroupsManager(negotiatorClient) {
  // const VIRTUAL_ORGANIZATION_ID = import.meta.env.VITE_PERUN_VO_ID
  // const BASE_URL = import.meta.env.VITE_PERUN_API_URL
  const ORGANIZATION_ID_ATTR = import.meta.env.VITE_PERUN_ORGANIZATION_ID_ATTR
  const RESOURCE_ID_ATTR = import.meta.env.VITE_PERUN_RESOURCE_ID_ATTR
  const ADMIN_ORGANIZATION_ID_ATTR = import.meta.env.VITE_PERUN_ADMIN_ORGANIZATION_ID_ATTR
  const ADMIN_RESOURCE_ID_ATTR = import.meta.env.VITE_PERUN_ADMIN_RESOURCE_ID_ATTR
  // const GROUP_ATTR_DEF = import.meta.env.VITE_PERUN_GROUP_ATTR_DEF
  // const USER_ATTR_DEF = import.meta.env.VITE_PERUN_USER_ATTR_DEF
  // const EMAIL_ATTR_ID = import.meta.env.VITE_PERUN_EMAIL_ATTR

  const organizationsTree = {}
  const managerGroupMappings = {}

  const getValueForAttribute = (group, attributeName) => {
    const attribute = group.attributes.find((attr) => attr.baseFriendlyName === attributeName)
    return attribute && attribute.value ? attribute.value.trim() : null
  }

  const isValueForAttributeNotEmpty = (group, attributeName) => {
    return getValueForAttribute(group, attributeName) != null
  }

  const isOrganizationGroup = (group) => {
    return isValueForAttributeNotEmpty(group, ORGANIZATION_ID_ATTR)
  }

  const isResourceGroup = (group) => {
    return isValueForAttributeNotEmpty(group, RESOURCE_ID_ATTR)
  }

  const isOrganizationManagerGroup = (group) => {
    return isValueForAttributeNotEmpty(group, ADMIN_ORGANIZATION_ID_ATTR)
  }

  const isResourceManagerGroup = (group) => {
    return isValueForAttributeNotEmpty(group, ADMIN_RESOURCE_ID_ATTR)
  }

  const getNegotiatorOrganizationIdFromPerunGroup = (group) => {
    return getValueForAttribute(group, ORGANIZATION_ID_ATTR).replaceAll('.', ':')
  }

  const getResourceIdFromGroup = (group) => {
    return getValueForAttribute(group, RESOURCE_ID_ATTR).replaceAll('.', ':')
  }

  const getOrganizationIdFromManagerGroup = (group) => {
    return getValueForAttribute(group, ADMIN_ORGANIZATION_ID_ATTR).replaceAll('.', ':')
  }

  const getResourceIdFromManagerGroup = (group) => {
    return getValueForAttribute(group, ADMIN_RESOURCE_ID_ATTR).replaceAll('.', ':')
  }

  const initOrGetOrganization = (perunGroupId) => {
    if (!(perunGroupId in organizationsTree)) {
      organizationsTree[perunGroupId] = {
        id: perunGroupId,
        resources: [],
      }
    }
    return organizationsTree[perunGroupId]
  }

  const init = async (perunGroups) => {
    for (const perunGroup of perunGroups.data) {
      if (isOrganizationGroup(perunGroup)) {
        const organization = initOrGetOrganization(perunGroup.id)
        organization.directoryId = getNegotiatorOrganizationIdFromPerunGroup(perunGroup)
      } else if (isResourceGroup(perunGroup)) {
        const organization = initOrGetOrganization(perunGroup.parentGroupId)
        organization.resources.push({
          id: perunGroup.id,
          parentId: perunGroup.parentGroupId,
          directoryId: getResourceIdFromGroup(perunGroup),
        })
      } else if (isOrganizationManagerGroup(perunGroup)) {
        managerGroupMappings[getOrganizationIdFromManagerGroup(perunGroup)] = perunGroup.id
      } else if (isResourceManagerGroup(perunGroup)) {
        managerGroupMappings[getResourceIdFromManagerGroup(perunGroup)] = perunGroup.id
      }
    }

    await Promise.all(
      perunGroups.data.filter(isOrganizationGroup).map(async (perunGroup) => {
        const organization = initOrGetOrganization(perunGroup.id)
        const negotiatorOrg = await negotiatorClient.getOrganizationByExternalId(
          organization.directoryId,
        )
        if (negotiatorOrg) {
          organization.negotiatorOrg = negotiatorOrg
          organization.negotiatorOrg.id = perunGroup.id
        }
      }),
    )
  }

  const isInitialized = () => {
    return Object.keys(organizationsTree).length > 0
  }

  const getOrganizationIdsInNegotiator = () => {
    return Object.values(organizationsTree).map((org) => org.directoryId)
  }

  const getOrganizations = () => {
    return Object.entries(organizationsTree)
  }

  const getResourcesInOrganization = (organizationId) => {
    return organizationsTree[organizationId].resources
  }

  const getManagerGroupIdFromResource = (resource) => {
    return managerGroupMappings[resource.sourceId]
  }

  const getNegotiatorOrganizations = () => {
    return Object.values(organizationsTree).map((org) => org.negotiatorOrg)
  }

  return {
    isInitialized,
    init,
    getOrganizations,
    getOrganizationIdsInNegotiator,
    getResourcesInOrganization,
    getManagerGroupIdFromResource,
    getNegotiatorOrganizations,
  }
}
