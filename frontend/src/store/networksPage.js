import {defineStore} from "pinia"
import axios from "axios"
import {apiPaths, getBearerHeaders} from "../config/apiPaths"
import {useNotificationsStore} from "./notifications"

export const useNetworksPageStore = defineStore("networksPage", () => {
    const notifications = useNotificationsStore()

    function retrieveNetworkNegotiations(networkId, size, pageNumber, filtersSortData) {
        const parameters = {
            headers: getBearerHeaders(),
            params: {
                size,
                page: pageNumber,
                status: filtersSortData.status,
                createdAfter: filtersSortData.dateStart,
                createdBefore: filtersSortData.dateEnd,
                sortBy: filtersSortData.sortBy,
                sortOrder: filtersSortData.sortDirection
            },
            paramsSerializer: {indexes: null}
        }
        return axios.get(`${apiPaths.BASE_API_PATH}/networks/${networkId}/negotiations`, parameters)
            .then((response) => {
                return response.data
            })
            .catch(() => {
                notifications.setNotification("Error fetching negotiations for the network")
                return []
            })
    }

    async function retrieveUserNetworks(userId) {
        return await axios.get(`${apiPaths.BASE_API_PATH}/users/${userId}/networks`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data._embedded.networks
            })
    }

    async function retrieveNetwork(networkId) {
        return await axios.get(`${apiPaths.BASE_API_PATH}/networks/${networkId}`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data
            })
            .catch(() => {
                notifications.setNotification("Error retrieving network")
                return {}
            })
    }

    async function retrieveNetworkStats(networkId, startDate, endDate) {
        return await axios.get(`${apiPaths.BASE_API_PATH}/networks/${networkId}/statistics?since=${startDate}&until=${endDate}`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data
            })
            .catch(() => {
                notifications.setNotification("Error retrieving network")
                return {}
            })
    }

    return {
        retrieveNetworkNegotiations,
        retrieveUserNetworks,
        retrieveNetwork,
        retrieveNetworkStats
    }
})
