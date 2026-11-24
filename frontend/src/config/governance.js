import { ROLES } from './consts'


const devSettings = {
  client: import.meta.env.VITE_GOVERNANCE_CLIENT,
  virtualOrganizationId: import.meta.env.VITE_PERUN_VO_ID,
  idsPrefix: import.meta.env.VITE_PERUN_IDS_PREFIX,
  organizationIdAttr: import.meta.env.VITE_PERUN_ORGANIZATION_ID_ATTR,
  resourceIdAttr: import.meta.env.VITE_PERUN_RESOURCE_ID_ATTR,
  adminOrganizationIdAttr: import.meta.env.VITE_PERUN_ADMIN_ORGANIZATION_ID_ATTR,
  adminResourceIdAttr: import.meta.env.VITE_PERUN_ADMIN_RESOURCE_ID_ATTR,
  groupAttrDef: import.meta.env.VITE_PERUN_GROUP_ATTR_DEF,
  userAttrDef: import.meta.env.VITE_PERUN_USER_ATTR_DEF,
  emailAttrId: import.meta.env.VITE_PERUN_EMAIL_ATTR,
}

const prodSettings = {
  client: 'GOVERNANCE_CLIENT_PLACEHOLDER',
  virtualOrganizationId: 'GOVERNANCE_PERUN_VIRTUAL_ORGANIZATION_PLACEHOLDER',
  idsPrefix: 'GOVERNANCE_PERUN_IDS_PREFIX_PLACEHOLDER',
  organizationIdAttr: 'GOVERNANCE_PERUN_ORGANIZATION_ID_ATTR_PLACEHOLDER',
  resourceIdAttr: 'GOVERNANCE_PERUN_RESOURCE_ID_ATTR_PLACEHOLDER',
  adminOrganizationIdAttr: 'GOVERNANCE_PERUN_ADMIN_ORGANIZATION_ID_ATTR_PLACEHOLDER',
  adminResourceIdAttr: 'GOVERNANCE_PERUN_ADMIN_RESOURCE_ID_ATTR_PLACEHOLDER',
  groupAttrDef: 'GOVERNANCE_PERUN_GROUP_ATTR_BASE_PLACEHOLDER',
  userAttrDef: 'GOVERNANCE_PERUN_USER_ATTR_BASE_PLACEHOLDER',
  emailAttrId: 'GOVERNANCE_PERUN_EMAIL_ATTR_PLACEHOLDER',
}

const governanceSettings = import.meta.env.DEV ? devSettings : prodSettings

export default {
  ...governanceSettings,
  managerRoles: governanceSettings.client === 'NEGOTIATOR'
    ? [ROLES.ADMINISTRATOR]
    : [ROLES.ADMINISTRATOR, ROLES.NETWORK_MANAGER],
}
