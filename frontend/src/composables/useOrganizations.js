import { ref, computed, onMounted } from 'vue'
import { useAdminStore } from '@/store/admin'
import { useOrganizationsStore } from '@/store/organizations'
import { sortOrganizations, sortResources } from '@/utils/sort'
import { getNoResultsMessage as buildNoResultsMsg } from '@/utils/messages'

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
    resourceName: '',
  })

  let searchTimeout = null

  const allExpanded = computed(() => {
    return (
      organizations.value.length > 0 &&
      expandedOrganizations.value.size === organizations.value.length
    )
  })

  const sortedOrganizations = computed(() => sortOrganizations(organizations.value || []))

  const sortedResourcesForOrganization = (organizationId) => {
    const resources = organizationResources.value[organizationId] || []
    return sortResources(resources)
  }

  const getNoResultsMessage = () => {
    const hasSearchFilters =
      !!filters.value.name || !!filters.value.externalId || !!filters.value.resourceName
    return buildNoResultsMsg(loading.value, hasSearchFilters, filters.value.statusFilter)
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
      organizations.value.forEach((org) => {
        expandedOrganizations.value.add(org.id)
      })
      await Promise.all(organizations.value.map((org) => loadResourcesForOrganization(org.id)))
    }
  }

  const loadResourcesForOrganization = async (organizationId) => {
    if (organizationResources.value[organizationId]) {
      return
    }

    loadingResources.value.add(organizationId)

    try {
      const organizationWithResources = await organizationsStore.getOrganizationById(
        organizationId,
        'resources',
      )
      const resources =
        organizationWithResources.resources || organizationWithResources._embedded?.resources || []
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
        apiFilters,
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
      resourceName: '',
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
    reloadResourcesForOrganization,
  }
}
