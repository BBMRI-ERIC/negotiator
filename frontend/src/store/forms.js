import {defineStore} from "pinia"
import axios from "axios"
import {apiPaths, getBearerHeaders} from "../config/apiPaths"
import {useNotificationsStore} from "./notifications"

export const useFormsStore = defineStore("forms", () => {
    const notifications = useNotificationsStore()

    function retrieveAccessFormById(id) {
        return axios.get(`${apiPaths.BASE_API_PATH}/access-forms/${id}`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data
            })
            .catch(() => {
                notifications.setNotification("Error getting request data from server")
                return null
            })
    }

    function retrieveAllAccessForms() {
        return axios.get(`${apiPaths.BASE_API_PATH}/access-forms`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data._embedded["access-forms"]
            })
            .catch(() => {
                notifications.setNotification("Error getting request data from server")
                return null
            })
    }

    function retrieveDynamicAccessFormsValueSetByLink(link) {
        return axios.get(`${link}`, {headers: getBearerHeaders()})
            .then((response) => {
                return response.data
            })
            .catch(() => {
                notifications.setNotification("Error getting value-sets request data from server")
                return null
            })
    }

    function submitRequiredInformation(data, negotiationId, requirementId) {
        return axios.post(`/api/v3/negotiations/${negotiationId}/info-requirements/${requirementId}`, data, {headers: getBearerHeaders()})
            .then((response) => {
                notifications.setNotification("Thank you. Your response was successfully submitted. ")
                return response.data.id
            })
            .catch(() => {
                notifications.setNotification("There was an error saving the Negotiation")
            })
    }

    return {
        retrieveAccessFormById,
        retrieveAllAccessForms,
        retrieveDynamicAccessFormsValueSetByLink,
        submitRequiredInformation
    }
})
