<template>
  <div v-if="isLoaded">
    <div class="container">
      <div class="organization-details">
        <div class="avatar">
          <i class="bi bi-people-fill mt-2"/>
        </div>
        <div class="organization-info ms-3">
          <h1 class="h2 lh-condensed">
            {{ network?.name?.toUpperCase() }}
          </h1>
          <ul class="list-style-none">
            <li>
              <i class="bi bi-globe"/>
              <a
                  :href="network.uri"
                  class="ms-2"
              >{{ network.uri }}</a>
            </li>
            <li>
              <i class="bi bi-envelope"/>
              <a
                  :href="'mailto:' + network.contactEmail"
                  class="ms-2"
              >{{ network.contactEmail }}</a>
            </li>
            <li>
              <i class="bi bi-clipboard"/>
              <div class="text-muted ms-2 text-nowrap">
                {{ network.externalId }}
              </div>
            </li>
          </ul>
        </div>
      </div>
      <!-- Tabs Navigation -->
      <ul class="nav nav-tabs">
        <li class="nav-item tab cursor-pointer">
          <a
              class="nav-link"
              :class="{ active: currentTab === 'overview' }"
              @click="currentTab = 'overview'"
          >
            Overview
          </a>
        </li>
        <li class="nav-item tab cursor-pointer">
          <a
              class="nav-link"
              :class="{ active: currentTab === 'negotiations' }"
              @click="currentTab = 'negotiations'"
          >
            Negotiations
          </a>
        </li>
      </ul>
      <div
          v-if="currentTab === 'overview'"
          class="mb-4"
      >
        <h4 class="mb-4 mt-5">
          <i class="bi bi-graph-up"/>
          Insights
        </h4>
        <p class="text-muted">
          The overview visible below is generated for the selected period
        </p>

        <!-- Date Range Filters -->
        <div class="mb-4">
          <div class="mb-4">
            <div class="form-group d-inline-block mr-3">
              <label
                  for="startDate"
                  class="form-label"
              >Start Date:</label>
              <input
                  id="startDate"
                  v-model="startDate"
                  type="date"
                  class="form-control"
              >
            </div>

            <div class="form-group d-inline-block mx-4">
              <label
                  for="endDate"
                  class="form-label"
              >End Date:</label>
              <input
                  id="endDate"
                  v-model="endDate"
                  type="date"
                  class="form-control"
              >
            </div>
          </div>
        </div>

        <!-- Requests Card -->
        <div class="card">
          <div class="card-body">
            <!-- Card Header -->
            <div class="d-flex flex-row mb-2 align-items-center">
              <h4 class="card-title mb-0">
                Requests
              </h4>
              <i
                  class="bi bi-info-circle ml-2 small-icon"
                  title="States of different Negotiations involving Resources in this Network"
              />
            </div>

            <!-- Total Number of Requests -->
            <div class="text-center mb-2">
              <h5>Total Requests: {{ stats.totalNumberOfNegotiations }}</h5>
            </div>

            <!-- Pie Chart Section -->
            <div
                v-if="stats"
                class="pie-chart-container"
            >
              <Pie
                  :data="pieData"
                  :options="pieOptions"
              />
            </div>
          </div>
        </div>

        <!-- Additional Information Card -->
        <div class="card mt-4">
          <div class="card-body">
            <!-- Card Header for Additional Information -->
            <div class="d-flex flex-row mb-4 align-items-center">
              <h4 class="card-title mb-0">
                Additional Information
              </h4>
              <i
                  class="bi bi-info-circle ml-2 small-icon"
                  title="Additional statistics related to negotiations"
              />
            </div>

            <!-- Additional Stats Information -->
            <div class="row mt-4">
              <div class="col-md-6 col-lg-4 mb-4 d-flex">
                <div class="stat-card flex-fill">
                  <div class="stat-label">
                    <span>Ignored Negotiations</span>
                    <i
                        class="bi bi-info-circle small-icon"
                        title="The number of negotiations that were ignored"
                    />
                  </div>
                  <h5>{{ stats.numberOfIgnoredNegotiations }}</h5>
                </div>
              </div>

              <div class="col-md-6 col-lg-4 mb-4 d-flex">
                <div class="stat-card flex-fill">
                  <div class="stat-label">
                    <span>Median Response Time</span>
                    <i
                        class="bi bi-info-circle small-icon"
                        title="Median time taken for negotiations to receive responses"
                    />
                  </div>
                  <h5 class="text-muted">
                    {{ stats.medianResponseTime || 'N/A' }}
                  </h5>
                </div>
              </div>

              <div class="col-md-6 col-lg-4 mb-4 d-flex">
                <div class="stat-card flex-fill">
                  <div class="stat-label">
                    <span>Successful Negotiations</span>
                    <i
                        class="bi bi-info-circle small-icon"
                        title="The number of successful negotiations"
                    />
                  </div>
                  <h5>{{ stats.numberOfSuccessfulNegotiations }}</h5>
                </div>
              </div>

              <div class="col-md-6 col-lg-4 mb-4 d-flex">
                <div class="stat-card flex-fill">
                  <div class="stat-label">
                    <span>New Requesters</span>
                    <i
                        class="bi bi-info-circle small-icon"
                        title="The number of new requesters in this network"
                    />
                  </div>
                  <h5>{{ stats.numberOfNewRequesters }}</h5>
                </div>
              </div>

              <div class="col-md-6 col-lg-4 mb-4 d-flex">
                <div class="stat-card flex-fill">
                  <div class="stat-label">
                    <span>Active Representatives</span>
                    <i
                        class="bi bi-info-circle small-icon"
                        title="The number of active representatives currently in the network"
                    />
                  </div>
                  <h5>{{ stats.numberOfActiveRepresentatives }}</h5>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div
          v-else-if="currentTab === 'negotiations'"
          class="mt-3"
      >
        <FilterSort
            v-if="isLoaded"
            :user-role="userRole"
            :filters-status="states"
            :filters-sort-data="filtersSortData"
            @filters-sort-data="retrieveLatestNegotiations"
        />
        <NegotiationList
            :negotiations="negotiations"
            :pagination="pagination"
            :network-activated="true"
            :filters-sort-data="filtersSortData"
            @filters-sort-data="retrieveLatestNegotiations"
        />
        <NegotiationPagination
            :negotiations="negotiations"
            :pagination="pagination"
            @current-page-number="retrieveLatestNegotiations"
        />
      </div>
    </div>
  </div>
  <LoadingSpinner v-else:/>
</template>

<script setup>
import {onMounted, ref, watch} from "vue"
import {useNetworksPageStore} from "@/store/networksPage"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import {useUserStore} from "@/store/user"
import FilterSort from "@/components/FilterSort.vue"
import NegotiationList from "@/components/NegotiationList.vue"
import NegotiationPagination from "@/components/NegotiationPagination.vue"
import {useNegotiationsStore} from "@/store/negotiations"
import {Pie} from "vue-chartjs"
import {ArcElement, CategoryScale, Chart as ChartJS, DoughnutController, Legend, Title, Tooltip} from "chart.js"
import {getPieChartBackgroundColor} from "../composables/utils.js";

ChartJS.register(Title, Tooltip, Legend, ArcElement, CategoryScale, DoughnutController)

// Pie chart data

const props = defineProps({
  networkId: {
    type: String,
    required: true
  }
})
const userStore = useUserStore()
const negotiationsStore = useNegotiationsStore()
const networksPageStore = useNetworksPageStore()
const network = ref(undefined)
const negotiations = ref(undefined)
const currentTab = ref("overview") // Default tab
const stats = ref(undefined)
const pagination = ref(undefined)
const states = ref(undefined)
const filtersSortData = ref({
  status: [],
  dateStart: "",
  dateEnd: "",
  sortBy: "creationDate",
  sortDirection: "DESC"
})
const today = new Date()
const startOfYear = new Date(today.getFullYear(), 0, 1)
const startDate = ref(startOfYear.toISOString().slice(0, 10))
const endDate = ref(today.toISOString().slice(0, 10))
const userRole = ref("author")
const pageNumber = ref(0)
const isLoaded = ref(false)
// Pie chart data
const pieData = ref({})
const pieOptions = ref({
  responsive: true,
  plugins: {
    legend: {
      position: "right",
      align: "center",
      labels: {
        boxWidth: 20,
        padding: 20
      }
    }
  }
})
onMounted(async () => {
  await userStore.retrieveUser()
})
watch(endDate, () => {
  loadStats(props.networkId)
}, {immediate: true})
watch(startDate, () => {
  loadStats(props.networkId)
}, {immediate: true})
watch(
    [network, states, stats],
    ([newNetwork, newStates, newStats]) => {
      isLoaded.value = !!(newNetwork && newStates && newStats)
    },
    {immediate: true} // Run the watcher immediately on component mount
)
loadNetworkInfo(props.networkId)
loadStats(props.networkId)
loadNegotiationStates()
retrieveLatestNegotiations()

async function loadNegotiationStates() {
  states.value = await negotiationsStore.retrieveNegotiationLifecycleStates()
}

function getLabelByValue(value) {
  const item = states.value.find(entry => entry.value === value)
  return item ? item.label : null // Returns null if the value is not found
}

async function loadNetworkInfo(networkId) {
  network.value = await networksPageStore.retrieveNetwork(networkId)
}

async function loadStats(networkId) {
  stats.value = await networksPageStore.retrieveNetworkStats(networkId, startDate.value, endDate.value)
  if (!stats.value.statusDistribution) {
    setPieData(Object.keys(stats.value.statusDistribution), Object.values(stats.value.statusDistribution))
  } else {
    setPieData(['Total Requests: 0'], [100])
  }
}

function setPieData(labelsData, datasetsData) {
  pieData.value = {
    labels: labelsData,
    datasets: [{
      data: datasetsData,
      backgroundColor: getPieChartBackgroundColor(),
      hoverOffset: 4
    }]
  }
}

async function retrieveLatestNegotiations(currentPageNumber) {
  if (currentPageNumber) {
    pageNumber.value = currentPageNumber - 1
  }
  const response = await networksPageStore.retrieveNetworkNegotiations(props.networkId, 50, pageNumber.value, filtersSortData.value)
  pagination.value = response.page
  if (response.page.totalElements === 0) {
    negotiations.value = {}
  } else {
    negotiations.value = response._embedded.negotiations
  }
}
</script>
<style scoped>
.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background-color: #e1e4e8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 50px;
  color: #ffffff;
}

.organization-details {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  margin-top: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
}

.organization-details img {
  margin-bottom: 1rem;
  margin-right: 1.5rem;
}

.organization-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.organization-info h1 {
  font-size: 1.75rem;
  font-weight: 600;
}

.organization-info .description {
  color: #586069;
}

.organization-info ul {
  list-style: none;
  padding: 0;
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 1rem;
  margin-bottom: 0;
}

.organization-info ul li {
  max-width: 230px;
  display: flex;
  align-items: center;
  color: #586069;
}

.organization-info ul li a {
  color: #0366d6;
  text-decoration: none;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  max-width: 100%;
}

.organization-info ul li a:hover {
  text-decoration: underline;
}

.organization-info ul li svg {
  margin-right: 0.5rem;
}

.container {
  width: 100%;
  max-width: 1200px; /* Optional: Set max width for better control */
  margin: 0 auto; /* Center the container */
  padding: 0 15px; /* Optional: Add padding for spacing */
}

.stat-box h5 {
  margin-bottom: 0;
}

.small-icon {
  font-size: 0.75rem;
}

.pie-chart-container {
  width: 100%; /* Ensure it fills its container */
  height: 300px; /* Fixed height */
  display: flex; /* Enable Flexbox */
  justify-content: center; /* Center horizontally */
  align-items: center; /* Center vertically */
  margin: auto; /* Optional: center the chart inside its parent */
}

.card {
  border-radius: 10px;
  border: 1px solid #dee2e6;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  background-color: #fff;
  width: 100%; /* Ensure card takes full width within container */
  box-sizing: border-box; /* Prevent overflow by including padding/border in width calculation */
}

.card-body {
  padding: 30px;
}

.card-title {
  font-size: 20px;
  color: #343a40;
}

.text-center h5 {
  font-size: 22px;
  color: #343a40;
  font-weight: 600;
}

.bi-info-circle {
  cursor: pointer;
  font-size: 18px;
  color: #007bff;
  transition: color 0.2s ease;
}

.bi-info-circle:hover {
  color: #0056b3;
}

.stat-card {
  padding: 20px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  background-color: #f8f9fa;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  width: 100%; /* Ensure full width of stat card */
}

.stat-card:hover {
  background-color: #e9ecef;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.stat-label {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #6c757d;
  margin-bottom: 8px;
}

.stat-card h5 {
  font-size: 20px;
  margin: 0;
  color: #343a40;
}

.card-body .row {
  margin-left: 0;
  margin-right: 0;
}

.d-flex {
  display: flex;
  flex-direction: row;
  gap: 1rem; /* Flex gap for child elements */
}

.flex-fill {
  flex: 1;
}

.text-muted {
  color: #6c757d !important;
}

.mt-3, .mt-4 {
  margin-top: 1.5rem !important;
}

.row {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  width: 100%;
}

.col-md-6, .col-lg-4 {
  flex: 1 1 45%; /* Adjust flex for responsiveness */
  max-width: 33%; /* Set max-width to prevent over-expansion */
}

</style>
