import governanceSettings from '@/config/governanceSettings'
import { ref } from 'vue'

const ORGANIZATION_ID_ATTR = governanceSettings.organization_id_attr
const RESOURCE_ID_ATTR = governanceSettings.resource_id_attr
const ADMIN_ORGANIZATION_ID_ATTR = governanceSettings.admin_organization_id_attr
const ADMIN_RESOURCE_ID_ATTR = governanceSettings.admin_reource_id_attr

const getValueForAttribute = (group, attributeName) => {
  const attribute = group.attributes.find((attr) => attr.baseFriendlyName === attributeName)
  return attribute && attribute.value ? attribute.value.trim() : null
}

const getResourceIdFromGroup = (group) => {
  return getValueForAttribute(group, RESOURCE_ID_ATTR).replaceAll('.', ':')
}

function OrganizationGroup() {
  const id = ref(null)
  const negotiatorId = ref(null)
  const negotiatorOrganization = ref(null)
  const resources = ref([])

  const fromOrganizationGroup = (perunGroup) => {
    id.value = perunGroup.id
  }

  const fromResourceGroup = (perunGroup) => {
    id.value = perunGroup.parentGroupId
  }

  const getPerunId = () => {
    return id.value
  }

  const setNegotiatorId = (newNegotiatorId) => {
    negotiatorId.value = newNegotiatorId
  }

  const getNegotiatorId = () => {
    return negotiatorId.value
  }

  const addResourceGroup = (resource) => {
    resources.value.push(resource)
  }

  const getResourcesGroup = () => {
    return resources.value
  }

  const setNegotiatorOrganization = (newNegotiatorOrganization) => {
    negotiatorOrganization.value = newNegotiatorOrganization
  }

  const getNegotiatorOrganization = () => {
    console.log(negotiatorOrganization.value)
    return negotiatorOrganization.value
  }

  return {
    fromOrganizationGroup,
    fromResourceGroup,
    getPerunId,
    setNegotiatorId,
    getNegotiatorId,
    addResourceGroup,
    getResourcesGroup,
    setNegotiatorOrganization,
    getNegotiatorOrganization,
    resources: resources.value,
  }
}

function ResourceGroup(perunGroup) {
  const id = ref(perunGroup.id)
  const negId = ref(getResourceIdFromGroup(perunGroup))
  const parentId = ref(perunGroup.parentGroupId)

  const getPerunId = () => {
    return id.value
  }

  const setNegotiatorId = (newNegotiatorId) => {
    negId.value = newNegotiatorId
  }

  const getNegotiatorId = () => {
    return negId.value
  }

  const getParentId = () => {
    return parentId.value
  }

  return {
    getPerunId,
    getParentId,
    setNegotiatorId,
    getNegotiatorId,
  }
}

function NegotiatorOrganization(pgId, org) {
  const id = pgId
  const perunGroupId = pgId

  return {
    id,
    perunGroupId,
    externalId: org.externalId,
    name: org.name,
    description: org.description,
    contactEmail: org.contactEmail,
    uri: org.uri,
    withdrawn: org.withdrawn,
  }
}

export function PerunGroupsManager(negotiatorClient) {
  const organizationsTree = {}
  const managerGroupMappings = {}

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

  const getOrganizationIdFromManagerGroup = (group) => {
    return getValueForAttribute(group, ADMIN_ORGANIZATION_ID_ATTR).replaceAll('.', ':')
  }

  const getResourceIdFromManagerGroup = (group) => {
    return getValueForAttribute(group, ADMIN_RESOURCE_ID_ATTR).replaceAll('.', ':')
  }

  const getOrganizationGroup = (perunGroupId) => {
    return organizationsTree[perunGroupId] || null
  }

  const getOrCreateOrganizationGroup = (perunGroupId) => {
    if (!(perunGroupId in organizationsTree)) {
      organizationsTree[perunGroupId] = OrganizationGroup(perunGroupId)
    }
    return organizationsTree[perunGroupId]
  }

  const init = async (perunGroups) => {
    for (const perunGroup of perunGroups.data) {
      if (isOrganizationGroup(perunGroup)) {
        const organizationGroup = getOrCreateOrganizationGroup(perunGroup.id)
        organizationGroup.setNegotiatorId(getNegotiatorOrganizationIdFromPerunGroup(perunGroup))
      } else if (isResourceGroup(perunGroup)) {
        const organizationGroup = getOrCreateOrganizationGroup(perunGroup.parentGroupId)

        const resourceGroup = ResourceGroup(perunGroup)
        organizationGroup.addResourceGroup(resourceGroup)
      } else if (isOrganizationManagerGroup(perunGroup)) {
        managerGroupMappings[getOrganizationIdFromManagerGroup(perunGroup)] = perunGroup.id
      } else if (isResourceManagerGroup(perunGroup)) {
        managerGroupMappings[getResourceIdFromManagerGroup(perunGroup)] = perunGroup.id
      }
    }

    await Promise.all(
      perunGroups.data.filter(isOrganizationGroup).map(async (perunGroup) => {
        const organization = getOrCreateOrganizationGroup(perunGroup.id)
        const negotiatorOrg = await negotiatorClient.getOrganizationByExternalId(
          organization.getNegotiatorId(),
        )
        if (negotiatorOrg) {
          organization.setNegotiatorOrganization(
            NegotiatorOrganization(perunGroup.id, negotiatorOrg),
          )
        }
      }),
    )
  }

  const isInitialized = () => {
    return Object.keys(organizationsTree).length > 0
  }

  const getOrganizationIdsInNegotiator = () => {
    return Object.values(organizationsTree).map((org) => org.getNegotiatorId())
  }

  const getResourcesInOrganization = (organizationId) => {
    return organizationsTree[organizationId].getResourcesGroup()
  }

  const getOrganizationGroupIdFromResource = (resource) => {
    return resource.perunParentGroupId
  }

  const getResourceGroupIdFromResource = (resource) => {
    return resource.perunGroupId
  }

  const getManagerGroupIdFromResource = (resource) => {
    console.log(resource)
    return managerGroupMappings[resource.sourceId]
  }

  const getNegotiatorOrganizations = () => {
    return Object.values(organizationsTree).map((org) => org.getNegotiatorOrganization())
  }

  return {
    isInitialized,
    init,
    getOrganizationIdsInNegotiator,
    getResourcesInOrganization,
    getOrganizationGroupIdFromResource,
    getResourceGroupIdFromResource,
    getManagerGroupIdFromResource,
    getNegotiatorOrganizations,
  }
}
