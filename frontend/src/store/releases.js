import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

export const useReleasesStore = defineStore('releases', () => {
  const releases = ref([])
  const isLoading = ref(false)
  const lastChecked = ref(null)
  const currentVersion = ref('')
  const dismissedReleases = ref(new Set())

  // Cache configuration
  const CACHE_DURATION = 60 * 60 * 1000 // 1 hour in milliseconds
  const CACHE_KEY = 'negotiator_releases'
  const LAST_CHECKED_KEY = 'negotiator_releases_last_checked'

  const latestRelease = computed(() => {
    return releases.value.length > 0 ? releases.value[0] : null
  })

  const hasNewRelease = computed(() => {
    if (!latestRelease.value || !currentVersion.value) return false

    // Only show notification for versions following v3.x.x pattern
    if (!isVersionPatternSupported(currentVersion.value)) return false

    const latest = latestRelease.value.tag_name
    const current = currentVersion.value

    // Skip if this release has been dismissed
    if (dismissedReleases.value.has(latest)) return false

    // Simple version comparison - you might want to use semver for more robust comparison
    return latest !== current && !latestRelease.value.draft && !latestRelease.value.prerelease
  })

  function isVersionPatternSupported(version) {
    // Check if version follows v3.x.x pattern (e.g., v3.17.3, v3.0.0, v3.1.2)
    const versionPattern = /^v3\.\d+\.\d+$/
    return versionPattern.test(version)
  }

  function loadCachedReleases() {
    try {
      const cachedData = localStorage.getItem(CACHE_KEY)
      const lastCheckedTime = localStorage.getItem(LAST_CHECKED_KEY)

      if (cachedData && lastCheckedTime) {
        const cacheAge = Date.now() - parseInt(lastCheckedTime)

        // Use cached data if it's less than 1 hour old
        if (cacheAge < CACHE_DURATION) {
          releases.value = JSON.parse(cachedData)
          lastChecked.value = new Date(parseInt(lastCheckedTime)).toISOString()
          console.log('Loaded releases from cache')
          return true
        }
      }
    } catch (error) {
      console.warn('Failed to load cached releases:', error)
    }
    return false
  }

  function saveCachedReleases() {
    try {
      localStorage.setItem(CACHE_KEY, JSON.stringify(releases.value))
      localStorage.setItem(LAST_CHECKED_KEY, Date.now().toString())
    } catch (error) {
      console.warn('Failed to save releases to cache:', error)
    }
  }

  async function fetchReleases(forceRefresh = false) {
    // If not forcing refresh, try to load from cache first
    if (!forceRefresh && loadCachedReleases()) {
      return
    }

    if (isLoading.value) return

    isLoading.value = true
    try {
      const response = await axios.get('https://api.github.com/repos/BBMRI-ERIC/negotiator/releases', {
        headers: {
          'Accept': 'application/vnd.github.v3+json'
        }
      })
      releases.value = response.data
      lastChecked.value = new Date().toISOString()

      // Save to cache
      saveCachedReleases()
      console.log('Fetched fresh releases from GitHub API')
    } catch (error) {
      console.warn('Failed to fetch releases:', error)
      // If API fails but we have cached data, use it even if stale
      loadCachedReleases()
    } finally {
      isLoading.value = false
    }
  }

  function setCurrentVersion(version) {
    currentVersion.value = version
  }

  function dismissRelease(tagName) {
    dismissedReleases.value.add(tagName)
    // Store in localStorage to persist across sessions
    localStorage.setItem('dismissedReleases', JSON.stringify([...dismissedReleases.value]))
  }

  function loadDismissedReleases() {
    try {
      const dismissed = localStorage.getItem('dismissedReleases')
      if (dismissed) {
        dismissedReleases.value = new Set(JSON.parse(dismissed))
      }
    } catch (error) {
      console.warn('Failed to load dismissed releases:', error)
    }
  }

  function clearCache() {
    try {
      localStorage.removeItem(CACHE_KEY)
      localStorage.removeItem(LAST_CHECKED_KEY)
      releases.value = []
      lastChecked.value = null
      console.log('Release cache cleared')
    } catch (error) {
      console.warn('Failed to clear cache:', error)
    }
  }

  // Check for releases with caching
  function startPeriodicCheck() {
    // Initial check (will use cache if available)
    fetchReleases()

    // Check every hour, but will use cache if still valid
    setInterval(() => {
      fetchReleases()
    }, CACHE_DURATION) // Check every hour
  }

  // Manual refresh function for debugging/admin purposes
  function forceRefresh() {
    return fetchReleases(true)
  }

  return {
    releases,
    isLoading,
    lastChecked,
    currentVersion,
    latestRelease,
    hasNewRelease,
    fetchReleases,
    setCurrentVersion,
    dismissRelease,
    loadDismissedReleases,
    startPeriodicCheck,
    clearCache,
    forceRefresh
  }
})
