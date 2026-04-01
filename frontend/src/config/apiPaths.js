import { useOidcStore } from '../store/oidc'
import governanceSettings from './governance'
const BASE_API_PATH = '/api/v3'
const BASE_PERUN_URL = governanceSettings.apiUrl

const apiPaths = {
  BASE_API_PATH: `${BASE_API_PATH}`,
  ACCESS_CRITERIA_PATH: `${BASE_API_PATH}/access-criteria`,
  REQUESTS_PATH: `${BASE_API_PATH}/requests`,
  NEGOTIATION_PATH: `${BASE_API_PATH}/negotiations`,
  USER_PATH: `${BASE_API_PATH}/userinfo`,
  USER_RESOURCES_PATH: `${BASE_API_PATH}/users/resources`,
  USER_NOTIFICATIONS_PATH: `${BASE_API_PATH}/users`,
  ATTACHMENTS_PATH: `${BASE_API_PATH}/attachments`,
  BACKEND_ACTUATOR_INFO_PATH: '/api/actuator/info',
  VALUE_SETS: `${BASE_API_PATH}/value-sets`,
}

const perunApiPaths = {
  GET_GROUPS: `${BASE_PERUN_URL}/groupsManager/getAllRichGroupsWithAttributesByNames`,
  GET_RICH_MEMBERS: `${BASE_PERUN_URL}/groupsManager/getGroupRichMembersWithAttributes`,
  GET_MEMBERS: `${BASE_PERUN_URL}/membersManager/getMembersPage`,
  ADD_MEMBER_TO_GROUP: `${BASE_PERUN_URL}/groupsManager/addMember`,
  REMOVE_MEMBER_TO_GROUP: `${BASE_PERUN_URL}/groupsManager/removeMember`,
}

function getBearerHeaders() {
  const oidcStore = useOidcStore()
  return { Authorization: `Bearer ${oidcStore.access_token}` }
}

export { apiPaths, perunApiPaths, getBearerHeaders }
