import {defineStore} from "pinia"
import axios from "axios"
import {apiPaths, getBearerHeaders} from "../config/apiPaths"
import {useNotificationsStore} from "./notifications"

export const useAdminStore = defineStore("admin", () => {
    const notifications = useNotificationsStore()

    function retrieveResourceAllEvents() {
        return axios.get(`${apiPaths.BASE_API_PATH}/resource-lifecycle/events`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data._embedded.events
            })
            .catch(() => {
                notifications.setNotification("Error getting all resource events data from server")
            })
    }

    function setInfoRequirements(data) {
        return axios.post(`${apiPaths.BASE_API_PATH}/info-requirements`, data, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data
            })
            .catch(() => {
                notifications.setNotification("Error sending message")
            })
    }

    function retrieveInfoRequirement(link) {
        return axios.get(`${link}`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data
            })
            .catch(() => {
                notifications.setNotification("Error getting Info Requirements data from server")
            })
    }

    function retrieveInfoRequirements() {
        return axios.get(`${apiPaths.BASE_API_PATH}/info-requirements`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data._embedded
            })
            .catch(() => {
                notifications.setNotification("Error getting Info Requirements data from server")
            })
    }

    return {
        retrieveResourceAllEvents,
        setInfoRequirements,
        retrieveInfoRequirement,
        retrieveInfoRequirements
    }
})
