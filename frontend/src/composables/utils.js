export function capitalizeFirstLetter(val) {
  return String(val).charAt(0).toUpperCase() + String(val).slice(1)
}

export function transformStatus(status) {
  if (status === 'SUBMITTED') {
    return 'UNDER REVIEW'
  }
  return status ? status.toUpperCase().split('_').join(' ') : ''
}

export function getBadgeColor(status) {
  switch (status) {
    case 'SUBMITTED':
      return 'bg-status-badge'
    case 'DECLINED':
    case 'ABANDONED':
      return 'bg-danger'
    case 'APPROVED':
    case 'PAUSED':
      return 'bg-info'
    case 'IN_PROGRESS':
      return 'bg-primary'
    case 'CONCLUDED':
      return 'bg-success'
    default:
      return 'bg-secondary' // Fallback in case a status is not recognized
  }
}

export function getButtonColor(event) {
  switch (event.toUpperCase()) {
    case 'APPROVE':
      return 'btn-success'
    case 'DECLINE':
      return 'btn-danger'
    case 'START':
      return 'btn-primary'
    case 'PAUSE':
      return 'btn-info'
    case 'UNPAUSE':
      return 'btn-primary' // Assuming the same color as "START"
    case 'ABANDON':
      return 'btn-danger'
    case 'CONCLUDE':
      return 'btn-success'
    default:
      return 'btn-info' // Fallback color for unknown events
  }
}

export function getButtonIcon(event) {
  switch (event.toUpperCase()) {
    case 'APPROVE':
      return 'bi bi-check-circle'
    case 'DECLINE':
      return 'bi bi-ban'
    case 'START':
      return 'bi bi-play-circle'
    case 'PAUSE':
      return 'bi bi-pause-circle'
    case 'UNPAUSE':
      return 'bi bi-play-circle-fill' // Assuming a different icon for unpause
    case 'ABANDON':
      return 'bi bi-x-circle'
    case 'CONCLUDE':
      return 'bi bi-check-circle-fill'
    default:
      return 'bi bi-question-circle' // Fallback for unknown events
  }
}

export function getBadgeIcon(status) {
  switch (status.toUpperCase()) {
    case 'DRAFT':
      return 'bi bi-pencil'
    case 'SUBMITTED':
      return 'bi bi-search'
    case 'DECLINED':
    case 'ABANDONED':
      return 'bi-x-circle'
    case 'APPROVED':
    case 'PAUSED':
      return 'bi-pause-circle'
    case 'IN_PROGRESS':
      return 'bi-circle'
    case 'CONCLUDED':
      return 'bi-check-circle'
    default:
      return 'bi-circle' // Fallback in case a status is not recognized
  }
}

export function getStatusColor(stateValue) {
  switch (stateValue) {
    case 'SUBMITTED':
      return 'bg-primary'
    case 'RESOURCE_UNAVAILABLE':
      return 'bg-danger'
    case 'ACCESS_CONDITIONS_INDICATED':
      return 'bg-warning'
    case 'RESOURCE_NOT_MADE_AVAILABLE':
      return 'bg-danger'
    case 'REPRESENTATIVE_CONTACTED':
      return 'bg-warning'
    case 'RETURNED_FOR_RESUBMISSION':
      return 'bg-warning'
    case 'RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT':
      return 'bg-secondary'
    case 'ACCESS_CONDITIONS_MET':
      return 'bg-success'
    case 'REPRESENTATIVE_UNREACHABLE':
      return 'bg-dark'
    case 'RESOURCE_AVAILABLE':
      return 'bg-success'
    case 'RESOURCE_MADE_AVAILABLE':
      return 'bg-success'
    case 'CHECKING_AVAILABILITY':
      return 'bg-info'
    default:
      return 'bg-secondary' // Fallback color
  }
}

export function getStatusIcon(stateValue) {
  switch (stateValue) {
    case 'SUBMITTED':
      return 'bi bi-hourglass' // Icon for submission
    case 'RESOURCE_UNAVAILABLE':
      return 'bi bi-x-circle' // Icon for unavailable resource
    case 'ACCESS_CONDITIONS_INDICATED':
      return 'bi bi-exclamation-circle' // Icon for access conditions indicated
    case 'RESOURCE_NOT_MADE_AVAILABLE':
      return 'bi bi-x-square' // Icon for resource not made available
    case 'REPRESENTATIVE_CONTACTED':
      return 'bi bi-person-lines-fill' // Icon for contacting representative
    case 'RETURNED_FOR_RESUBMISSION':
      return 'bi bi-arrow-repeat' // Icon for resubmission
    case 'RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT':
      return 'bi bi-box-seam' // Icon for willing to collect
    case 'ACCESS_CONDITIONS_MET':
      return 'bi bi-check-all' // Icon for access conditions met
    case 'REPRESENTATIVE_UNREACHABLE':
      return 'bi bi-person-x' // Icon for unreachable representative
    case 'RESOURCE_AVAILABLE':
      return 'bi bi-check' // Icon for available resource
    case 'RESOURCE_MADE_AVAILABLE':
      return 'bi bi-check-circle' // Icon for resource made available
    case 'CHECKING_AVAILABILITY':
      return 'bi bi-search' // Icon for checking availability
    default:
      return 'bi bi-question-circle' // Fallback icon
  }
}

export function generatePieChartBackgroundColorArray(labelsArray) {
  const pieChartBackgroundColorArray = []
  labelsArray.forEach((label) => {
    switch (label) {
      case 'SUBMITTED':
        // "bg-status-badge" bootstrap theme color
        pieChartBackgroundColorArray.push('#f37125')
        break
      case 'DECLINED':
      case 'ABANDONED':
        // "bg-danger" bootstrap theme color
        pieChartBackgroundColorArray.push('#dc3545')
        break
      case 'APPROVED':
      case 'PAUSED':
        // "bg-info" bootstrap theme color
        pieChartBackgroundColorArray.push('#7c7c7c')
        break
      case 'IN_PROGRESS':
        // "bg-primary" bootstrap theme color
        pieChartBackgroundColorArray.push('#26336B')
        break
      case 'CONCLUDED':
        // "bg-success" bootstrap theme color
        pieChartBackgroundColorArray.push('#3B8501')
        break
      default:
        // "bg-secondary" bootstrap theme color
        pieChartBackgroundColorArray.push('#26336B') // Fallback in case a status is not recognized
    }
  })

  return pieChartBackgroundColorArray
}

import fileExtensions from '@/config/uploadFileExtensions.js'
import { useNotificationsStore } from '../store/notifications'
import moment from 'moment'

export function isFileExtensionsSupported(file) {
  const notificationsStore = useNotificationsStore()
  const fileExtensionsArray = fileExtensions.split(', ').map((item) => item.trim())

  if (fileExtensionsArray.includes(file['type'])) {
    return true
  }
  notificationsStore.setNotification(
    'Invalid file type. Please upload a file with a valid extension.',
    'danger',
  )
  return false
}

/**
 * Formats a UTC timestamp to local timezone with relative time (e.g., "2 hours ago")
 * @param {string} utcTimestamp - UTC timestamp from the server
 * @returns {string} Formatted relative time in user's local timezone
 */
export function formatTimestampToLocal(utcTimestamp) {
  if (!utcTimestamp) return ''
  const isLocalDev = import.meta.env.DEV === true || window.location.hostname === 'localhost'
  if (isLocalDev) {
    return moment(utcTimestamp).fromNow()
  } else {
    return moment.utc(utcTimestamp).local().fromNow()
  }
}

/**
 * Formats a UTC timestamp to a readable local date and time
 * @param {string} utcTimestamp - UTC timestamp from the server
 * @returns {string} Formatted date and time in user's local timezone (European format)
 */
export function formatTimestampToLocalDateTime(utcTimestamp) {
  if (!utcTimestamp) return ''
  const isLocalDev = import.meta.env.DEV === true || window.location.hostname === 'localhost'

  let localMoment
  if (isLocalDev) {
    localMoment = moment(utcTimestamp)
  } else {
    localMoment = moment.utc(utcTimestamp).local()
  }

  return localMoment.format('DD.MM.YYYY HH:mm')
}

/**
 * Formats a timestamp using moment.js without localization or timezone conversion
 * @param {string} timestamp - Timestamp to format
 * @param {string} format - Moment.js format string (default: 'MMM D, YYYY HH:mm')
 * @returns {string} Formatted timestamp without localization
 */
export function formatTimestamp(timestamp, format = 'MMM D, YYYY HH:mm') {
  if (!timestamp) return '-'
  return moment.utc(timestamp).format(format) + ' UTC'
}
