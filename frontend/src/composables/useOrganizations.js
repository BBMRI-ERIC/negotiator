import { ref, computed, onMounted } from 'vue'
import { useAdminStore } from '@/store/admin'
import { useOrganizationsStore } from '@/store/organizations'

export function useOrganizations() {
  const adminStore = useAdminStore()
  const organizationsStore = useOrganizationsStore()

  const organizations = ref([])
  const organizationResources = ref({})
  const loadingResources = ref(new Set())
  const expandedOrganizations = ref(new Set())
  const loading = ref(true)
  const pageNumber = ref(0)
  const totalPages = ref(0)
  const totalElements = ref(0)
  const pageLinks = ref({})
  const pageSize = ref(20)

  const filters = ref({
    statusFilter: 'active',
    name: '',
    externalId: '',
    resourceName: ''
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

  const toggleExpandAll = async () => {
    if (allExpanded.value) {
      expandedOrganizations.value.clear()
    } else {
      organizations.value.forEach(org => {
        expandedOrganizations.value.add(org.id)
      })
      await Promise.all(
        organizations.value.map(org => loadResourcesForOrganization(org.id))
      )
    }
  }

  const loadResourcesForOrganization = async (organizationId) => {
    if (organizationResources.value[organizationId]) {
      return
    }

    loadingResources.value.add(organizationId)

    try {
      const organizationWithResources = await organizationsStore.getOrganizationById(organizationId, 'resources')
      const resources = organizationWithResources.resources || organizationWithResources._embedded?.resources || []
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

      if (filters.value.externalId && filters.value.externalId.trim()) {
        apiFilters.externalId = filters.value.externalId.trim()
      }

      if (filters.value.statusFilter === 'active') {
        apiFilters.withdrawn = false
      } else if (filters.value.statusFilter === 'withdrawn') {
        apiFilters.withdrawn = true
      }

      const response = await adminStore.retrieveOrganizationsPaginated(
        pageNumber.value,
        pageSize.value,
        apiFilters
      )
      organizations.value = response?._embedded?.organizations ?? []
      pageLinks.value = response._links || {}
      pageNumber.value = response.page?.number ?? 0
      totalPages.value = response.page?.totalPages ?? 0
      totalElements.value = response.page?.totalElements ?? 0
    } catch (error) {
      console.error('Error loading organizations:', error)
      organizations.value = []
    } finally {
      loading.value = false
    }
  }

  const previousPage = () => {
    if (pageNumber.value > 0) {
      pageNumber.value -= 1
      loadOrganizations()
    }
  }

  const nextPage = () => {
    if (pageNumber.value < totalPages.value - 1) {
      pageNumber.value += 1
      loadOrganizations()
    }
  }

  const updatePageSize = (newPageSize) => {
    if (newPageSize < 1) {
      pageSize.value = 20
    } else if (newPageSize > 100) {
      pageSize.value = 100
    } else {
      pageSize.value = newPageSize
    }
    pageNumber.value = 0
    loadOrganizations()
  }

  const updateFilters = (newFilters) => {
    Object.assign(filters.value, newFilters)
  }

  const clearFilters = () => {
    filters.value = {
      statusFilter: 'active',
      name: '',
      externalId: '',
      resourceName: ''
    }
    pageNumber.value = 0
    loadOrganizations()
  }

  const applyFilters = () => {
    pageNumber.value = 0
    loadOrganizations()
  }

  const debouncedSearch = () => {
    if (searchTimeout) {
      clearTimeout(searchTimeout)
    }

    searchTimeout = setTimeout(() => {
      pageNumber.value = 0
      loadOrganizations()
    }, 500)
  }

  const reloadResourcesForOrganization = async (organizationId) => {
    if (organizationId) {
      delete organizationResources.value[organizationId]
      await loadResourcesForOrganization(organizationId)
    }
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
    pageNumber,
    totalPages,
    totalElements,
    pageSize,
    filters,
    allExpanded,
    getNoResultsMessage,
    sortedResourcesForOrganization,
    toggleOrganization,
    toggleExpandAll,
    loadResourcesForOrganization,
    loadOrganizations,
    previousPage,
    nextPage,
    updatePageSize,
    updateFilters,
    clearFilters,
    applyFilters,
    debouncedSearch,
    reloadResourcesForOrganization
  }
}
