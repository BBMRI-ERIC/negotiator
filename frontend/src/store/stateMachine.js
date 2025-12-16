import { defineStore } from 'pinia'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '../config/apiPaths'
import { useNotificationsStore } from './notifications'

export const useStateMachineStore = defineStore('stateMachine', () => {
  const notifications = useNotificationsStore()

  function createStateMachine(config) {
    return axios
      .post(`${apiPaths.BASE_API_PATH}/state-machines`, config, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        notifications.setNotification('State machine created successfully!')
        return response.data
      })
      .catch((error) => {
        const errorMessage = error.response?.data?.message || error.message || 'Error creating state machine'
        notifications.setNotification(errorMessage)
        throw error
      })
  }

  function getAllStateMachines() {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/state-machines`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error fetching state machines')
        throw error
      })
  }

  function getStateMachineById(id) {
    return axios
      .get(`${apiPaths.BASE_API_PATH}/state-machines/${id}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error fetching state machine details')
        throw error
      })
  }

  function updateStateMachine(id, config) {
    return axios
      .put(`${apiPaths.BASE_API_PATH}/state-machines/${id}`, config, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        notifications.setNotification('State machine updated successfully!')
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error updating state machine')
        throw error
      })
  }

  function deleteStateMachine(id) {
    return axios
      .delete(`${apiPaths.BASE_API_PATH}/state-machines/${id}`, {
        headers: getBearerHeaders(),
      })
      .then((response) => {
        notifications.setNotification('State machine deleted successfully!')
        return response.data
      })
      .catch((error) => {
        notifications.setNotification('Error deleting state machine')
        throw error
      })
  }

  return {
    createStateMachine,
    getAllStateMachines,
    getStateMachineById,
    updateStateMachine,
    deleteStateMachine,
  }
})

