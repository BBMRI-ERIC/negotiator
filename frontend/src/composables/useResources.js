import { ref, computed, onMounted } from 'vue'
import { useResourcesStore } from '@/store/resources'
import { useUserStore } from '@/store/user.js'

export function useResources() {
  const resourceStore = useResourcesStore()
  const userStore = useUserStore()

  const organizations = ref([])
  const organizationResources = ref({})
  const loadingResources = ref(new Set())
  const expandedOrganizations = ref(new Set())
  const loading = ref(true)

  const filters = ref({
    statusFilter: 'active',
    name: ''
  })

  let searchTimeout = null

  const allExpanded = computed(() => {
    return organizations.value.length > 0 && expandedOrganizations.value.size === organizations.value.length
  })

  const sortedOrganizations = computed(() => {
    if (!organizations.value || organizations.value.length === 0) {
      return []
    }

    const activeOrgs = organizations.value.filter(org => !org.withdrawn)
    const withdrawnOrgs = organizations.value.filter(org => org.withdrawn)

    const sortedActive = activeOrgs.sort((a, b) => {
      const aId = String(a.id || a.externalId || '').toLowerCase()
      const bId = String(b.id || b.externalId || '').toLowerCase()
      return aId.localeCompare(bId, undefined, { numeric: true })
    })

    const sortedWithdrawn = withdrawnOrgs.sort((a, b) => {
      const aId = String(a.id || a.externalId || '').toLowerCase()
      const bId = String(b.id || b.externalId || '').toLowerCase()
      return aId.localeCompare(bId, undefined, { numeric: true })
    })

    return [...sortedActive, ...sortedWithdrawn]
  })

  const sortedResourcesForOrganization = (organizationId) => {
    const resources = organizationResources.value[organizationId] || []
    if (resources.length === 0) return []

    const activeResources = resources.filter(resource => !resource.withdrawn && resource.status !== 'withdrawn')
    const withdrawnResources = resources.filter(resource => resource.withdrawn || resource.status === 'withdrawn')

    const sortedActive = activeResources.sort((a, b) => {
      const aId = String(a.id || a.sourceId || '').toLowerCase()
      const bId = String(b.id || b.sourceId || '').toLowerCase()
      return aId.localeCompare(bId, undefined, { numeric: true })
    })

    const sortedWithdrawn = withdrawnResources.sort((a, b) => {
      const aId = String(a.id || a.sourceId || '').toLowerCase()
      const bId = String(b.id || b.sourceId || '').toLowerCase()
      return aId.localeCompare(bId, undefined, { numeric: true })
    })

    return [...sortedActive, ...sortedWithdrawn]
  }

  const getNoResultsMessage = () => {
    if (loading.value) {
      return 'Loading organizations...'
    }

    const hasSearchFilters = filters.value.name || filters.value.externalId || filters.value.resourceName

    if (hasSearchFilters) {
      return 'No organizations found matching your search criteria.'
    }

    if (filters.value.statusFilter === 'withdrawn') {
      return 'No withdrawn organizations found.'
    } else if (filters.value.statusFilter === 'active') {
      return 'No active organizations found.'
    }

    return 'No organizations found.'
  }

  const toggleOrganization = async (organizationId) => {
    if (expandedOrganizations.value.has(organizationId)) {
      expandedOrganizations.value.delete(organizationId)
    } else {
      expandedOrganizations.value.add(organizationId)
      await loadResourcesForOrganization(organizationId)
    }
  }

  const loadResourcesForOrganization = async (organizationId) => {
    if (organizationResources.value[organizationId]) {
      return
    }

    loadingResources.value.add(organizationId)

    try {
      const resources = organizations.value.find(org => org.id === organizationId)?.resources || []

      organizationResources.value[organizationId] = resources
    } catch (error) {
      console.error('Failed to load resources for organization:', organizationId, error)
      organizationResources.value[organizationId] = []
    } finally {
      loadingResources.value.delete(organizationId)
    }
  }

  const loadOrganizations = async () => {
    loading.value = true
    try {
      const apiFilters = {}

      if (filters.value.name && filters.value.name.trim()) {
        apiFilters.name = filters.value.name.trim()
      }

      if (filters.value.statusFilter === 'active') {
        apiFilters.withdrawn = false
      } else if (filters.value.statusFilter === 'withdrawn') {
        apiFilters.withdrawn = true
      }

      const response = await resourceStore.getRepresentedResources(
        userStore.userInfo?.id,
        false,
        apiFilters
      )

      organizations.value = response
    } catch (error) {
      console.error('Error loading organizations:', error)
      organizations.value = []
    } finally {
      loading.value = false
    }
  }

  const updateFilters = (newFilters) => {
    Object.assign(filters.value, newFilters)
  }

  const clearFilters = () => {
    filters.value = {
      statusFilter: 'active',
      name: ''
    }
    loadOrganizations()
  }

  const applyFilters = () => {
    loadOrganizations()
  }

  const debouncedSearch = () => {
    if (searchTimeout) {
      clearTimeout(searchTimeout)
    }

    searchTimeout = setTimeout(() => {
      loadOrganizations()
    }, 500)
  }

  onMounted(() => {
    loadOrganizations()
  })

  return {
    organizations: sortedOrganizations,
    organizationResources,
    loadingResources,
    expandedOrganizations,
    loading,
    filters,
    allExpanded,
    getNoResultsMessage,
    sortedResourcesForOrganization,
    toggleOrganization,
    loadResourcesForOrganization,
    loadOrganizations,
    updateFilters,
    clearFilters,
    applyFilters,
    debouncedSearch
  }
}
