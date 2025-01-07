import {defineStore} from "pinia"
import axios from "axios"
import {apiPaths, getBearerHeaders} from "../config/apiPaths"
import {useNotificationsStore} from "./notifications"

export const useNegotiationsStore = defineStore("negotiations", () => {
    const notifications = useNotificationsStore()

    async function retrieveNegotiations(filtersSortData, pageNumber) {
        const parameters = {
            headers: getBearerHeaders(),
            params: {
                status: filtersSortData.status,
                createdAfter: filtersSortData.dateStart,
                createdBefore: filtersSortData.dateEnd,
                sortBy: filtersSortData.sortBy,
                sortOrder: filtersSortData.sortDirection,
                page: pageNumber
            },
            paramsSerializer: {indexes: null}
        }
        return await axios.get(`${apiPaths.BASE_API_PATH}/negotiations`, parameters)
            .then((response) => {
                return response.data
            })
            .catch((error) => {
                notifications.criticalError = true
                notifications.setNotification(error.response.status + " " + error.response.statusText)
                return []
            })
    }

    async function retrieveNegotiationsByUserId(role, filtersSortData, userId, pageNumber) {
        const parameters = {
            headers: getBearerHeaders(),
            params: {
                role,
                status: filtersSortData.status,
                createdAfter: filtersSortData.dateStart,
                createdBefore: filtersSortData.dateEnd,
                sortBy: filtersSortData.sortBy,
                sortOrder: filtersSortData.sortDirection,
                page: pageNumber
            },
            paramsSerializer: {indexes: null}
        }
        return await axios.get(`${apiPaths.BASE_API_PATH}/users/${userId}/negotiations`, parameters)
            .then((response) => {
                return response.data
            })
            .catch((error) => {
                notifications.criticalError = true
                notifications.setNotification(error.response.status + " " + error.response.statusText)
                return []
            })
    }

    async function retrieveNegotiationLifecycleStates() {
        return await axios.get(`${apiPaths.BASE_API_PATH}/negotiation-lifecycle/states`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data._embedded.states
            })
            .catch(() => {
                notifications.setNotification("Error getting negotiation lifecycle states request data from server")
                return null
            })
    }

    return {retrieveNegotiations, retrieveNegotiationsByUserId, retrieveNegotiationLifecycleStates}
})
