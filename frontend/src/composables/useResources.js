import { ref, computed, onMounted } from 'vue'
import { useResourcesStore } from '@/store/resources'
import { useUserStore } from '@/store/user.js'
import { sortOrganizations, sortResources } from '@/utils/sort'
import { getNoResultsMessage as buildNoResultsMsg } from '@/utils/messages'

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

  const sortedOrganizations = computed(() => sortOrganizations(organizations.value || []))

  const sortedResourcesForOrganization = (organizationId) => {
    const resources = organizationResources.value[organizationId] || []
    return sortResources(resources)
  }

  const getNoResultsMessage = () => {
    return buildNoResultsMsg(loading.value, !!filters.value.name, filters.value.statusFilter)
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
