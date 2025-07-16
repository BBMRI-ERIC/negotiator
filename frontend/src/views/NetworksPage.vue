<template>
  <negotiations-list-modal
    id="negotiationsModal"
    title="List of Negotiations"
    :negotiations="negotiationIds"
  />
  <div v-if="isLoaded">
    <div class="container">
      <NetworkHeader :network="network" />

      <NetworkWarningBanner :resources-without-representatives="resourcesWithoutRepresentatives" />

      <NetworkTabNavigation v-model:current-tab="currentTab" />

      <NetworkOverviewTab
        v-if="currentTab === 'overview'"
        :stats="stats"
        v-model:start-date="startDate"
        v-model:end-date="endDate"
        :pie-data="pieData"
        :pie-options="pieOptions"
        @set-negotiation-ids="setNegotiationIds"
      />

      <NetworkNegotiationsTab
        v-if="currentTab === 'negotiations'"
        :negotiations="negotiations || []"
        :pagination="pagination || {}"
        :states="states || []"
        :organizations="organizations"
        :filters-sort-data="filtersSortData"
        :user-role="userRole"
        :is-loaded="negotiationsLoaded"
        :loading="negotiationsLoading"
        @retrieve-negotiations-by-sort-and-filter="retrieveNegotiationsBySortAndFilter"
        @retrieve-negotiations-by-page="retrieveNegotiationsByPage"
      />

      <NetworkOrganizationsTab
        v-if="currentTab === 'organizations'"
        :organizations="organizations"
        :loading="organizationsLoading"
      />
    </div>
  </div>
  <LoadingSpinner v-else />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import moment from 'moment'
import { useNetworksPageStore } from '@/store/networksPage'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { useUserStore } from '@/store/user'
import { useNegotiationsStore } from '@/store/negotiations'
import {
  ArcElement,
  CategoryScale,
  Chart as ChartJS,
  DoughnutController,
  Legend,
  Title,
  Tooltip,
} from 'chart.js'
import { generatePieChartBackgroundColorArray } from '../composables/utils.js'
import NegotiationsListModal from '@/components/modals/NegotiationsListModal.vue'
import NetworkHeader from '@/components/NetworkHeader.vue'
import NetworkWarningBanner from '@/components/NetworkWarningBanner.vue'
import NetworkTabNavigation from '@/components/NetworkTabNavigation.vue'
import NetworkOverviewTab from '@/components/NetworkOverviewTab.vue'
import NetworkNegotiationsTab from '@/components/NetworkNegotiationsTab.vue'
import NetworkOrganizationsTab from '@/components/NetworkOrganizationsTab.vue'

ChartJS.register(Title, Tooltip, Legend, ArcElement, CategoryScale, DoughnutController)

const props = defineProps({
  networkId: {
    type: String,
    required: true,
  },
})

const route = useRoute()
const router = useRouter()

const userStore = useUserStore()
const negotiationsStore = useNegotiationsStore()
const networksPageStore = useNetworksPageStore()
const network = ref(undefined)
const negotiations = ref(undefined)

// Initialize tab from URL query parameter, default to 'overview'
const currentTab = ref(route.query.tab || 'overview')

const stats = ref(undefined)
const pagination = ref(undefined)
const states = ref(undefined)
const filtersSortData = ref({
  status: [],
  organizations: [],
  dateStart: '',
  dateEnd: '',
  sortBy: 'creationDate',
  sortDirection: 'DESC',
})

// Loading states for each tab
const organizationsLoaded = ref(false)
const negotiationsLoaded = ref(false)
const organizationsLoading = ref(false)
const negotiationsLoading = ref(false)

const resourcesWithoutRepresentatives = computed(() => {
  return organizations.value.reduce(
    (sum, org) =>
      sum +
      org.resources.filter(
        (resource) => resource.representatives.length === 0 && resource.withdrawn === false,
      ).length,
    0,
  )
})

const today = new Date()
const startOfYear = new Date(today.getFullYear(), 0, 1)
const startDate = ref(startOfYear.toISOString().slice(0, 10))
const endDate = ref(today.toISOString().slice(0, 10))
const userRole = ref('author')
const isLoaded = ref(false)
const organizations = ref([])
const negotiationIds = ref([])
// Pie chart data
const pieData = ref({})
const pieOptions = ref({
  responsive: true,
  plugins: {
    legend: {
      position: 'right',
      align: 'center',
      labels: {
        boxWidth: 20,
        padding: 20,
      },
    },
  },
  onClick: (evt, array) => {
    displayNegotiationsWithStatus(Object.keys(stats.value.statusDistribution)[array[0]?.index])
  },
})

onMounted(async () => {
  await userStore.retrieveUser()

  // Load data for the initial tab from URL
  if (currentTab.value === 'organizations' && !organizationsLoaded.value) {
    await loadOrganizations(props.networkId)
  } else if (currentTab.value === 'negotiations' && !negotiationsLoaded.value) {
    await loadNegotiationsData()
  }
})

// Watch for tab changes and update URL
watch(currentTab, async (newTab) => {
  // Update URL without triggering navigation
  await router.replace({
    query: { ...route.query, tab: newTab }
  })

  // Load data lazily
  if (newTab === 'organizations' && !organizationsLoaded.value && !organizationsLoading.value) {
    await loadOrganizations(props.networkId)
  } else if (newTab === 'negotiations' && !negotiationsLoaded.value && !negotiationsLoading.value) {
    await loadNegotiationsData()
  }
})

// Watch route query changes (e.g., browser back/forward)
watch(() => route.query.tab, (newTab) => {
  if (newTab && newTab !== currentTab.value) {
    currentTab.value = newTab
  }
})

watch(
  endDate,
  () => {
    loadStats(props.networkId)
  },
  { immediate: true },
)

watch(
  startDate,
  () => {
    loadStats(props.networkId)
  },
  { immediate: true },
)

watch(
  [network, stats],
  ([newNetwork, newStats]) => {
    isLoaded.value = !!(newNetwork && newStats)
  },
  { immediate: true },
)

// Initial load - only load essential data for overview tab
loadNetworkInfo(props.networkId)

async function loadOrganizations(networkId) {
  if (organizationsLoading.value) return

  organizationsLoading.value = true
  try {
    const response = await networksPageStore.retrieveNetworkOrganizations(networkId)
    if (response._embedded) {
      organizations.value = response._embedded.organizations
    }
    organizationsLoaded.value = true
  } catch (error) {
    console.error('Error loading organizations:', error)
  } finally {
    organizationsLoading.value = false
  }
}

async function loadNegotiationsData() {
  if (negotiationsLoading.value) return

  negotiationsLoading.value = true
  try {
    await Promise.all([
      loadNegotiationStates(),
      retrieveLatestNegotiations(0)
    ])
    negotiationsLoaded.value = true
  } catch (error) {
    console.error('Error loading negotiations data:', error)
  } finally {
    negotiationsLoading.value = false
  }
}

async function loadNegotiationStates() {
  if (!states.value) {
    states.value = await negotiationsStore.retrieveNegotiationLifecycleStates()
  }
}

async function loadNetworkInfo(networkId) {
  network.value = await networksPageStore.retrieveNetwork(networkId)
}

function setNegotiationIds(ids) {
  negotiationIds.value = ids
}

async function loadStats(networkId) {
  stats.value = await networksPageStore.retrieveNetworkStats(
    networkId,
    startDate.value,
    endDate.value,
  )

  if (Object.keys(stats.value.statusDistribution).length > 0) {
    setPieData(
      Object.keys(stats.value.statusDistribution),
      Object.values(stats.value.statusDistribution),
    )
  } else {
    setPieData(['Total Requests: 0'], [100])
  }
}

function setPieData(labelsData, datasetsData) {
  pieData.value = {
    labels: labelsData,
    datasets: [
      {
        data: datasetsData,
        backgroundColor: generatePieChartBackgroundColorArray(labelsData),
        hoverOffset: 4,
      },
    ],
  }
}

async function retrieveLatestNegotiations(currentPageNumber) {
  const response = await networksPageStore.retrieveNetworkNegotiations(
    props.networkId,
    50,
    currentPageNumber,
    filtersSortData.value,
  )
  pagination.value = response.page
  if (response.page.totalElements === 0) {
    negotiations.value = {}
  } else {
    negotiations.value = response._embedded.negotiations
  }
}

function incriseDateEndIfSame() {
  if (
    filtersSortData.value.dateStart &&
    filtersSortData.value.dateStart === filtersSortData.value.dateEnd
  ) {
    filtersSortData.value.dateEnd = moment(filtersSortData.value.dateEnd)
      .add(1, 'days')
      .format('YYYY-MM-DD')
  }
}

function retrieveNegotiationsBySortAndFilter() {
  incriseDateEndIfSame()
  retrieveLatestNegotiations(0)
}

function retrieveNegotiationsByPage(currentPageNumber) {
  retrieveLatestNegotiations(currentPageNumber - 1)
}

async function displayNegotiationsWithStatus(status) {
  if (status) {
    // Ensure negotiations data is loaded before switching tab
    if (!negotiationsLoaded.value) {
      await loadNegotiationsData()
    }

    filtersSortData.value.status = [status]
    filtersSortData.value.dateStart = startDate.value
    filtersSortData.value.dateEnd = moment(endDate.value).add(1, 'days').format('YYYY-MM-DD')
    currentTab.value = 'negotiations'

    retrieveNegotiationsBySortAndFilter()
  }
}
</script>

<style scoped>
.container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}
</style>
