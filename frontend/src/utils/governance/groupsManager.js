import governanceSettings from '@/config/governance'

const ORGANIZATION_ID_ATTR = governanceSettings.organizationIdAttr
const RESOURCE_ID_ATTR = governanceSettings.resourceIdAttr

const isValueForAttributeNotEmpty = (group, attributeName) => {
  return getValueForAttribute(group, attributeName) != undefined
}

const isOrganizationRepresentativesGroup = (group) => {
  return isValueForAttributeNotEmpty(group, ORGANIZATION_ID_ATTR)
}

const isResourceRepresentativesGroup = (group) => {
  return isValueForAttributeNotEmpty(group, RESOURCE_ID_ATTR)
}

const getValueForAttribute = (group, attributeName) => {
  const attribute = group.attributes.find((attr) => attr.baseFriendlyName === attributeName)
  return attribute?.value?.trim()
}

const getNegotiatorResourceIdFromRepresentativeGroup = (group, idsPrefix) => {
  return getValueForAttribute(group, RESOURCE_ID_ATTR).replaceAll(idsPrefix, '').replaceAll('.', ':')
}

const getNegotiatorOrganizationIdFromRepresentativeGroup = (group, idsPrefix) => {
  return getValueForAttribute(group, ORGANIZATION_ID_ATTR)
    .replaceAll(idsPrefix, '')
    .replaceAll('.', ':')
}

class OrganizationRepresentativeGroup {
  constructor(perunGroupId) {
    this.id = perunGroupId
    this.negotiatorId = null
    this.negotiatorOrganization = null
    this.resources = []
  }

  getPerunId() {
    return this.id
  }

  setNegotiatorId(newNegotiatorId) {
    this.negotiatorId = newNegotiatorId
  }

  getNegotiatorId() {
    return this.negotiatorId
  }

  addResourceGroup(resource) {
    this.resources.push(resource)
  }

  getResourcesGroup() {
    return this.resources
  }

  setNegotiatorOrganization(newNegotiatorOrganization) {
    this.negotiatorOrganization = newNegotiatorOrganization
  }

  getNegotiatorOrganization() {
    return this.negotiatorOrganization
  }
}

class ResourceRepresentativeGroup {
  constructor(perunGroup, idsPrefix) {
    this.id = perunGroup.id
    this.negotiatorId = getNegotiatorResourceIdFromRepresentativeGroup(perunGroup, idsPrefix)
    this.parentId = perunGroup.parentGroupId
    this.managerGroupId = null
  }

  getPerunId() {
    return this.id
  }

  setNegotiatorId(newNegotiatorId) {
    this.negotiatorId = newNegotiatorId
  }

  getNegotiatorId() {
    return this.negotiatorId
  }

  getParentId() {
    return this.parentId
  }

  setManagerGroupId(newManagerGroupId) {
    this.managerGroupId = newManagerGroupId
  }

  getManagerGroupId() {
    return this.managerGroupId
  }
}

function NegotiatorOrganization(representativeGroup, negotiatorData) {
  return {
    id: negotiatorData.id,
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
    id: negotiatorData.id,
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

function PerunGroupsManager(idsPrefix) {
  const groupsTree = {}

  let ready = false

  const getOrCreateOrganizationGroup = (perunGroupId) => {
    if (!(perunGroupId in groupsTree)) {
      groupsTree[perunGroupId] = new OrganizationRepresentativeGroup(perunGroupId)
    }
    return groupsTree[perunGroupId]
  }

  const deleteOrganizationGroup = (perunGroupId) => {
    delete groupsTree[perunGroupId]
  }

  const isGroupsTreeBuilt = () => {
    return Object.keys(groupsTree).length > 0
  }

  const isReady = () => {
    return ready
  }

  const markReady = () => {
    ready = true
  }

  const reset = () => {
    for (const perunGroupId of Object.keys(groupsTree)) {
      delete groupsTree[perunGroupId]
    }
    ready = false
  }

  const init = (perunGroups) => {
    if (!isGroupsTreeBuilt()) {
      for (const perunGroup of perunGroups.data) {
        if (isOrganizationRepresentativesGroup(perunGroup)) {
          const organizationGroup = getOrCreateOrganizationGroup(perunGroup.id)
          organizationGroup.setNegotiatorId(
            getNegotiatorOrganizationIdFromRepresentativeGroup(perunGroup, idsPrefix),
          )
        } else if (isResourceRepresentativesGroup(perunGroup)) {
          const organizationGroup = getOrCreateOrganizationGroup(perunGroup.parentGroupId)
          const resourceGroup = new ResourceRepresentativeGroup(perunGroup, idsPrefix)
          organizationGroup.addResourceGroup(resourceGroup)
        }
      }
    }
  }

  const getOrganizationsRepresentativesGroups = () => {
    return Object.values(groupsTree)
  }

  const getOrganizationGroupByNegotiatorId = (negotiatorId) => {
    if (negotiatorId === null || negotiatorId === undefined) {
      return null
    }
    return (
      Object.values(groupsTree).find(
        (group) => String(group.getNegotiatorOrganization()?.id) === String(negotiatorId),
      ) ?? null
    )
  }

  const getResourcesRepresentativesGroupsForOrganization = (organizationId) => {
    const organizationGroup = getOrganizationGroupByNegotiatorId(organizationId)
    return organizationGroup ? organizationGroup.getResourcesGroup() : []
  }

  const getNegotiatorOrganizationById = (organizationId) => {
    const organizationGroup = getOrganizationGroupByNegotiatorId(organizationId)
    return organizationGroup ? organizationGroup.getNegotiatorOrganization() : null
  }

  const getNegotiatorOrganizations = () => {
    return Object.values(groupsTree).map((org) => org.getNegotiatorOrganization())
  }

  return {
    isGroupsTreeBuilt,
    isReady,
    markReady,
    init,
    reset,
    getOrganizationsRepresentativesGroups,
    getNegotiatorOrganizationById,
    getResourcesRepresentativesGroupsForOrganization,
    getNegotiatorOrganizations,
    deleteOrganizationGroup,
  }
}

export { PerunGroupsManager, NegotiatorOrganization, NegotiatorResource }
