import { ROLES } from './consts'

const managerRoles =
  import.meta.env.VITE_GOVERNANCE_CLIENT == 'NEGOTIATOR'
    ? [ROLES.ADMINISTRATOR]
    : [ROLES.ADMINISTRATOR, ROLES.NETWORK_MANAGER]

const devSettings = {
  managerRoles: managerRoles,
  client: import.meta.env.VITE_GOVERNANCE_CLIENT,
  apiUrl: import.meta.env.VITE_PERUN_API_URL,
  virtualOrganizationId: import.meta.env.VITE_PERUN_VO_ID,
  organizationIdAttr: import.meta.env.VITE_PERUN_ORGANIZATION_ID_ATTR,
  resourceIdAttr: import.meta.env.VITE_PERUN_RESOURCE_ID_ATTR,
  adminOrganizationIdAttr: import.meta.env.VITE_PERUN_ADMIN_ORGANIZATION_ID_ATTR,
  adminResourceIdAttr: import.meta.env.VITE_PERUN_ADMIN_RESOURCE_ID_ATTR,
  groupAttrDef: import.meta.env.VITE_PERUN_GROUP_ATTR_DEF,
  userAttrDef: import.meta.env.VITE_PERUN_USER_ATTR_DEF,
  emailAttrId: import.meta.env.VITE_PERUN_EMAIL_ATTR,
}

const prodSettings = {
  managerRoles: managerRoles,
  client: 'GOVERNANCE_CLIENT_PLACEHOLDER',
  apiUrl: 'OIDC_API_PLACEHOLDER',
  virtualOrganizationId: 'VIRTUAL_ORGANIZATION_PLACEHOLDER',
  organizationIdAttr: 'ORGANIZATION_ID_ATTR_PLACEHOLDER',
  resourceIdAttr: 'RESOURCE_ID_ATTR_PLACEHOLDER',
  adminOrganizationIdAttr: 'ADMIN_ORGANIZATION_ID_ATTR_PLACEHOLDER',
  adminResourceIdAttr: 'ADMIN_RESOURCE_ID_ATTR_PLACEHOLDER',
  groupAttrDef: 'GROUP_ATTR_BASE_PLACEHOLDER',
  userAttrDef: 'USER_ATTR_BASE_PLACEHOLDER',
  emailAttrId: 'EMAIL_ATTR_PLACEHOLDER',
}

const governanceSettings = import.meta.env.DEV ? devSettings : prodSettings

export default governanceSettings
