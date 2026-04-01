import governanceSettings from '@/config/governance'
import { ref } from 'vue'

const ORGANIZATION_ID_ATTR = governanceSettings.organizationIdAttr
const RESOURCE_ID_ATTR = governanceSettings.resourceIdAttr
const ADMIN_RESOURCE_ID_ATTR = governanceSettings.adminResourceIdAttr

const isValueForAttributeNotEmpty = (group, attributeName) => {
  return getValueForAttribute(group, attributeName) != null
}

const isOrganizationRepresentativesGroup = (group) => {
  return isValueForAttributeNotEmpty(group, ORGANIZATION_ID_ATTR)
}

const isResourceRepresentativesGroup = (group) => {
  return isValueForAttributeNotEmpty(group, RESOURCE_ID_ATTR)
}

const isResourceManagersGroup = (group) => {
  return isValueForAttributeNotEmpty(group, ADMIN_RESOURCE_ID_ATTR)
}

const getValueForAttribute = (group, attributeName) => {
  const attribute = group.attributes.find((attr) => attr.baseFriendlyName === attributeName)
  return attribute && attribute.value ? attribute.value.trim() : null
}

const getNegotiatorResourceIdFromRepresentativeGroup = (group) => {
  return getValueForAttribute(group, RESOURCE_ID_ATTR).replaceAll('.', ':')
}

const getNegotiatorOrganizationIdFromRepresentativeGroup = (group) => {
  return getValueForAttribute(group, ORGANIZATION_ID_ATTR).replaceAll('.', ':')
}

const getNegoatiatorResourceIdFromManagerGroup = (group) => {
  return getValueForAttribute(group, ADMIN_RESOURCE_ID_ATTR).replaceAll('.', ':')
}

function OrganizationRepresentativeGroup(perunGroupId) {
  const id = ref(perunGroupId)
  const negotiatorId = ref(null)
  const negotiatorOrganization = ref(null)
  const resources = ref([])

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
    return negotiatorOrganization.value
  }

  return {
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

function ResourceRepresentativeGroup(perunGroup) {
  const id = ref(perunGroup.id)
  const negId = ref(getNegotiatorResourceIdFromRepresentativeGroup(perunGroup))
  const parentId = ref(perunGroup.parentGroupId)
  const managerGroupId = ref(null)

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

  const setManagerGroupId = (newManagerGroupId) => {
    managerGroupId.value = newManagerGroupId
  }

  const getManagerGroupId = () => {
    return managerGroupId.value
  }

  return {
    getPerunId,
    getParentId,
    setNegotiatorId,
    getNegotiatorId,
    setManagerGroupId,
    getManagerGroupId,
  }
}

function NegotiatorOrganization(representativeGroup, negotiatorData) {
  return {
    id: representativeGroup.getPerunId(),
    perunGroupId: representativeGroup.getPerunId(),
    externalId: negotiatorData.externalId,
    name: negotiatorData.name,
    description: negotiatorData.description,
    contactEmail: negotiatorData.contactEmail,
    uri: negotiatorData.uri,
    withdrawn: negotiatorData.withdrawn,
  }
}

function NegotiatorResource(representativeGroup, negotiatorData, representativesData) {
  const perunGroupId = representativeGroup.getPerunId()
  const perunParentGroupId = representativeGroup.getParentId()
  const managerGroupId = representativeGroup.getManagerGroupId()

  const getOrganizationGroupId = () => {
    return perunParentGroupId
  }

  const getResourceGroupId = () => {
    return perunGroupId
  }

  const getManagerGroupId = () => {
    return managerGroupId
  }

  return {
    id: perunGroupId,
    perunGroupId,
    perunParentGroupId,
    sourceId: negotiatorData.sourceId,
    name: negotiatorData.name,
    description: negotiatorData.description,
    contactEmail: negotiatorData.contactEmail,
    uri: negotiatorData.uri,
    withdrawn: negotiatorData.withdrawn,
    representatives: representativesData,
    getOrganizationGroupId,
    getResourceGroupId,
    getManagerGroupId,
  }
}

function PerunGroupsManager() {
  const groupsTree = {}

  const getOrCreateOrganizationGroup = (perunGroupId) => {
    if (!(perunGroupId in groupsTree)) {
      groupsTree[perunGroupId] = OrganizationRepresentativeGroup(perunGroupId)
    }
    return groupsTree[perunGroupId]
  }

  const isInitialized = () => {
    return Object.keys(groupsTree).length > 0
  }

  const init = (perunGroups) => {
    if (!isInitialized()) {
      const managerGroupResources = {}
      for (const perunGroup of perunGroups.data) {
        if (isOrganizationRepresentativesGroup(perunGroup)) {
          const organizationGroup = getOrCreateOrganizationGroup(perunGroup.id)
          organizationGroup.setNegotiatorId(
            getNegotiatorOrganizationIdFromRepresentativeGroup(perunGroup),
          )
        } else if (isResourceRepresentativesGroup(perunGroup)) {
          const organizationGroup = getOrCreateOrganizationGroup(perunGroup.parentGroupId)
          const resourceGroup = ResourceRepresentativeGroup(perunGroup)
          organizationGroup.addResourceGroup(resourceGroup)
          if (resourceGroup.getNegotiatorId() in managerGroupResources) {
            resourceGroup.setManagerGroupId(managerGroupResources[resourceGroup.getNegotiatorId()])
          } else {
            managerGroupResources[resourceGroup.getNegotiatorId()] = resourceGroup
          }
        } else if (isResourceManagersGroup(perunGroup)) {
          const resourceId = getNegoatiatorResourceIdFromManagerGroup(perunGroup)
          if (resourceId in managerGroupResources) {
            managerGroupResources[resourceId].setManagerGroupId(perunGroup.id)
          } else {
            managerGroupResources[resourceId] = perunGroup.id
          }
        }
      }
    }
  }

  const getOrganizationsRepresentativesGroups = () => {
    return Object.values(groupsTree)
  }

  const getResourcesRepresentativesGroupsForOrganization = (organizationId) => {
    return groupsTree[organizationId].getResourcesGroup()
  }

  const getNegotiatorOrganizations = () => {
    return Object.values(groupsTree).map((org) => org.getNegotiatorOrganization())
  }

  return {
    isInitialized,
    init,
    getOrganizationsRepresentativesGroups,
    getResourcesRepresentativesGroupsForOrganization,
    getNegotiatorOrganizations,
  }
}

export { PerunGroupsManager, NegotiatorOrganization, NegotiatorResource }
