const devSettings = {
  client: import.meta.env.VITE_GOVERNANCE_CLIENT,
  api_url: import.meta.env.VITE_PERUN_API_URL,
  virtual_organization_id: import.meta.env.VITE_PERUN_VO_ID,
  organization_id_attr: import.meta.env.VITE_PERUN_ORGANIZATION_ID_ATTR,
  resource_id_attr: import.meta.env.VITE_PERUN_RESOURCE_ID_ATTR,
  admin_organization_id_attr: import.meta.env.VITE_PERUN_ADMIN_ORGANIZATION_ID_ATTR,
  admin_resource_id_attr: import.meta.env.VITE_PERUN_ADMIN_RESOURCE_ID_ATTR,
  group_attr_def: import.meta.env.VITE_PERUN_GROUP_ATTR_DEF,
  user_attr_def: import.meta.env.VITE_PERUN_USER_ATTR_DEF,
  email_attr_id: import.meta.env.VITE_PERUN_EMAIL_ATTR,
}

const prodSettings = {
  client: 'GOVERNANCE_CLIENT_PLACEHOLDER',
  api_url: 'OIDC_API_PLACEHOLDER',
  virtual_organization_id: 'VIRTUAL_ORGANIZATION_PLACEHOLDER',
  organization_id_attr: 'ORGANIZATION_ID_ATTR_PLACEHOLDER',
  resource_id_attr: 'RESOURCE_ID_ATTR_PLACEHOLDER',
  admin_organization_id_attr: 'ADMIN_ORGANIZATION_ID_ATTR_PLACEHOLDER',
  admin_resource_id_attr: 'ADMIN_RESOURCE_ID_ATTR_PLACEHOLDER',
  group_attr_def: 'GROUP_ATTR_BASE_PLACEHOLDER',
  user_attr_def: 'USER_ATTR_BASE_PLACEHOLDER',
  email_attr_id: 'EMAIL_ATTR_PLACEHOLDER',
}

let governanceSettings

if (import.meta.env.DEV) {
  governanceSettings = devSettings
} else {
  governanceSettings = prodSettings
}

export default governanceSettings
