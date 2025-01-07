import {onMounted, ref} from "vue"
import {defineStore} from "pinia"
import axios from "axios"
import {apiPaths, getBearerHeaders} from "../config/apiPaths"
import {useNotificationsStore} from "./notifications"

export const useUiConfiguration = defineStore("uiConfiguration", () => {
    const notifications = useNotificationsStore()
    const uiConfiguration = ref({})

    function retrieveUiConfiguration() {
        return axios.get(`${apiPaths.BASE_API_PATH}/ui-config`)
            .then((response) => {
                uiConfiguration.value = response.data
                return response.data
            })
            .catch(() => {
                notifications.setNotification("Error getting UI Configuration data from server", "danger")
                return null
            })
    }

    function updateUiConfiguration(UiConfiguration) {
        return axios.put(`${apiPaths.BASE_API_PATH}/ui-config`, UiConfiguration, {headers: getBearerHeaders()})
            .then(() => {
                notifications.setNotification("Thank you. Your Ui-Configuration was submitted successfully", "success")
            })
            .catch(() => {
                notifications.setNotification("Error updating Ui-Configuration status", "danger")
                return null
            })
    }

    onMounted(async () => {
        if (Object.keys(uiConfiguration.value).length === 0) {
            await retrieveUiConfiguration()
        }
    })

    return {
        uiConfiguration,
        retrieveUiConfiguration,
        updateUiConfiguration
    }
})
