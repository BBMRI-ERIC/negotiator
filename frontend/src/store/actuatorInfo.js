import { ref } from 'vue'
import { defineStore } from 'pinia'
import axios from "axios"
import { apiPaths } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useActuatorInfoStore = defineStore("actuatorInfo", () => {
  const notifications = useNotificationsStore()

  const actuatorInfo = ref({})
  const actuatorInfoBuildVersion = ref('unknown')
  const actuatorInfoApplicationEnvironment = ref('')

  function retrieveBackendActuatorInfo() {
    return axios.get(apiPaths.BACKEND_ACTUATOR_INFO_PATH)
      .then((response) => {
        actuatorInfo.value = response.data
        actuatorInfoBuildVersion.value = response.data.build.version
        actuatorInfoApplicationEnvironment.value = response.data.application.environment
      })
      .catch((error) => {
        if (error.response) {
          notifications.setNotification('Error getting Backend Actuator Info data')
        }
      })
  }

  return { actuatorInfo, actuatorInfoBuildVersion, actuatorInfoApplicationEnvironment, retrieveBackendActuatorInfo }
})
