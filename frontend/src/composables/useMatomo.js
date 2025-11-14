import { useRouter } from 'vue-router'

/**
 * Composable for Matomo analytics tracking
 * Provides easy-to-use methods for tracking custom events and user actions
 */
export function useMatomo() {
  const router = useRouter()

  const isMatomoAvailable = () => {
    return typeof window._paq !== 'undefined'
  }

  /**
   * Track a custom event/action
   * @param {string} category - Event category (e.g., 'Negotiation', 'Form')
   * @param {string} action - Event action (e.g., 'Submit', 'Cancel')
   * @param {string} name - Event name/label (optional)
   * @param {number} value - Event value (optional)
   */
  const trackEvent = (category, action, name = '', value = 0) => {
    if (!isMatomoAvailable()) return

    if (import.meta.env.DEV) {
      console.log('[Matomo Event]', { category, action, name, value })
    }

    window._paq.push(['trackEvent', category, action, name, value])
  }

  /**
   * Track a goal completion
   * @param {number} goalId - Matomo goal ID
   * @param {number} value - Conversion value (optional)
   */
  const trackGoal = (goalId, value = 0) => {
    if (!isMatomoAvailable()) return

    if (import.meta.env.DEV) {
      console.log('[Matomo Goal]', { goalId, value })
    }

    window._paq.push(['trackGoal', goalId, value])
  }

  /**
   * Track a page view with custom parameters
   * @param {string} pageTitle - Page title
   * @param {string} pagePath - Page path (optional, defaults to current route)
   */
  const trackPageView = (pageTitle, pagePath = null) => {
    if (!isMatomoAvailable()) return

    const path = pagePath || router.currentRoute.value.path

    if (import.meta.env.DEV) {
      console.log('[Matomo PageView]', { pageTitle, pagePath: path })
    }

    window._paq.push(['setDocumentTitle', pageTitle])
    window._paq.push(['trackPageView'])
  }

  /**
   * Set custom variable for current or future page views
   * @param {number} index - Variable index (1-5)
   * @param {string} name - Variable name
   * @param {string} value - Variable value
   * @param {string} scope - Scope: 'page' or 'visit' (default: 'page')
   */
  const setCustomVariable = (index, name, value, scope = 'page') => {
    if (!isMatomoAvailable()) return

    if (import.meta.env.DEV) {
      console.log('[Matomo CustomVariable]', { index, name, value, scope })
    }

    window._paq.push(['setCustomVariable', index, name, value, scope])
  }

  /**
   * Track an error
   * @param {Error} error - Error object
   * @param {string} context - Error context/source
   */
  const trackError = (error, context = '') => {
    if (!isMatomoAvailable()) return

    const errorMessage = `${context}: ${error.message || String(error)}`

    if (import.meta.env.DEV) {
      console.log('[Matomo Error]', { error: errorMessage })
    }

    window._paq.push(['trackEvent', 'Error', 'Exception', errorMessage])
  }

  /**
   * Set user ID (for user identification across devices)
   * @param {string} userId - Unique user identifier
   */
  const setUserId = (userId) => {
    if (!isMatomoAvailable()) return

    if (import.meta.env.DEV) {
      console.log('[Matomo UserId]', { userId })
    }

    window._paq.push(['setUserId', userId])
  }

  return {
    trackEvent,
    trackGoal,
    trackPageView,
    setCustomVariable,
    trackError,
    setUserId,
    isMatomoAvailable,
  }
}

