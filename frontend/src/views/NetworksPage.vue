<template>
  <div v-if="isLoaded">
    <div class="container">
      <div class="organization-details">
        <div class="avatar">
          <i class="bi bi-people-fill mt-2" />
        </div>
        <div class="organization-info ms-3">
          <h1 class="h2 lh-condensed">
            {{ network?.name?.toUpperCase() }}
          </h1>
          <ul class="list-style-none">
            <li>
              <i class="bi bi-globe" />
              <a :href="network.uri" class="ms-2">{{ network.uri }}</a>
            </li>
            <li>
              <i class="bi bi-envelope" />
              <a :href="'mailto:' + network.contactEmail" class="ms-2">{{
                network.contactEmail
              }}</a>
            </li>
            <li>
              <i class="bi bi-clipboard" />
              <div class="text-muted ms-2 text-nowrap">
                {{ network.externalId }}
              </div>
            </li>
          </ul>
        </div>
        <div>
          <div class="d-flex flex-column">
            <a
              type="button"
              class="btn btn-sm mb-2"
              :href="externalLinks.auth_management_link"
              :style="{
      'background-color': uiConfiguration?.buttonColor,
      'color': '#ffffff'
    }"
            >
    <span>
      <i class="bi bi-gear"></i>
      Manage Resource Representatives
    </span>
            </a>
            <a
              type="button"
              class="btn btn-sm"
              href='https://bbmri-eric.github.io/negotiator/representative'
              :style="{
      'background-color': uiConfiguration?.buttonColor,
      'color': '#ffffff'
    }"
            >
    <span>
      <i class="bi bi-book"></i>
      Guide
    </span>
            </a>
          </div>

        </div>
      </div>
      <!-- Warning Banner -->
      <div v-if="resourcesWithoutRepresentatives > 0" class="alert alert-warning mt-3" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        Warning: At least one or more Resources in your Network do not have a single <strong>Representative</strong>
        attached to them.
        Hence, any new requests for these Resources <strong>will not be forwarded</strong>. To see which Resources need
        to be updated, go to the <strong>Organizations</strong> tab.
        To update them, click on the <strong>"Manage Resource Representatives"</strong> button above.
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
        <li class="nav-item tab cursor-pointer">
          <a
            class="nav-link"
            :class="{ active: currentTab === 'organizations' }"
            @click="currentTab = 'organizations'"
          >
            Organizations
          </a>
        </li>
      </ul>
      <div v-if="currentTab === 'overview'" class="mb-4">
        <h4 class="mb-4 mt-5">
          <i class="bi bi-graph-up" />
          Insights
        </h4>
        <p class="text-muted">The overview visible below is generated for the selected period</p>

        <!-- Date Range Filters -->
        <div class="mb-4">
          <div class="mb-4">
            <div class="form-group d-inline-block mr-3">
              <label for="startDate" class="form-label">Start Date:</label>
              <input id="startDate" v-model="startDate" type="date" class="form-control" />
            </div>

            <div class="form-group d-inline-block mx-4">
              <label for="endDate" class="form-label">End Date:</label>
              <input id="endDate" v-model="endDate" type="date" class="form-control" />
            </div>
          </div>
        </div>

        <!-- Requests Card -->
        <div class="card">
          <div class="card-body">
            <!-- Card Header -->
            <div class="d-flex flex-row mb-2 align-items-center">
              <h4 class="card-title mb-0">Requests</h4>
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
            <div v-if="stats" class="pie-chart-container">
              <Pie :data="pieData" :options="pieOptions" />
            </div>
          </div>
        </div>

        <div class="card mt-4">
          <div class="card-body">
            <!-- Card Header for Additional Information -->
            <div class="d-flex flex-row mb-4 align-items-center">
              <h4 class="card-title mb-0">Status Distribution</h4>
              <i
                class="bi bi-info-circle ml-2 small-icon"
                title="Statistics showing the distribution of negotiation statuses"
              ></i>
            </div>

            <!-- Dynamically Generated Status Cards -->
            <div class="row mt-4">
              <div
                v-for="(count, status) in stats.statusDistribution"
                :key="status"
                class="col-md-6 col-lg-4 mb-4 d-flex"
              >
                <div class="stat-card flex-fill">
                  <div class="stat-label">
                    <span>{{ formatStatusLabel(status) }}</span>
                    <i
                      class="bi bi-info-circle small-icon"
                      :title="'The number of negotiations with status: ' + formatStatusLabel(status)"
                    ></i>
                  </div>
                  <h5>{{ count }}</h5>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Additional Information Card -->
        <div class="card mt-4">
          <div class="card-body">
            <!-- Card Header for Additional Information -->
            <div class="d-flex flex-row mb-4 align-items-center">
              <h4 class="card-title mb-0">Additional Information</h4>
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

      <div v-if="currentTab === 'negotiations'" class="mt-3">
        <FilterSort
          v-if="isLoaded"
          :user-role="userRole"
          :filters-status="states"
          v-model:filtersSortData="filtersSortData"
          @filters-sort-data="retrieveNegotiationsBySortAndFilter"
        />
        <NegotiationList
          :negotiations="negotiations"
          :pagination="pagination"
          :network-activated="true"
          v-model:filtersSortData="filtersSortData"
          @filters-sort-data="retrieveNegotiationsBySortAndFilter"
        />
        <NegotiationPagination
          :negotiations="negotiations"
          :pagination="pagination"
          @current-page-number="retrieveNegotiationsByPage"
        />
      </div>
      <div v-if="currentTab === 'organizations'" class="mt-3">
        <div class="summary-section mb-4 p-3 bg-light border rounded">
          <h4>Summary</h4>
          <ul class="list-unstyled mb-0">
            <li>
              <strong>Total Organizations:</strong> {{ organizations.length }}
            </li>
            <li>
              <strong>Total Resources:</strong> {{ totalResources }}
            </li>
            <li>
              <strong>Resources Without Representatives:</strong> {{ resourcesWithoutRepresentatives }}
            </li>
          </ul>
        </div>

        <div>
          <div
            v-for="organization in organizations"
            :key="organization.id"
            class="card mb-3 position-relative"
          >
            <!-- Organization Header -->
            <div class="card-header d-flex justify-content-between align-items-center">
              <h5 class="mb-0">
                {{ organization.name }} ({{ organization.externalId }})
              </h5>
              <!-- Status Icon -->
              <div>
                <i
                  v-if="allResourcesHaveRepresentatives(organization.resources)"
                  class="bi bi-check-circle-fill text-success"
                  title="All resources have representatives"
                ></i>
                <i
                  v-else
                  class="bi bi-exclamation-triangle-fill text-warning"
                  title="At least one resource has no representative"
                ></i>
              </div>
            </div>

            <!-- Organization Body -->
            <div class="card-body">
              <!-- Loading Spinner -->
              <div v-if="loading" class="d-flex justify-content-center">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">Loading...</span>
                </div>
              </div>

              <p v-if="!loading">{{ organization.description }}</p>

              <ul v-if="!loading" class="list-unstyled">
                <li>
                  <i class="bi bi-envelope"></i>
                  <a :href="'mailto:' + organization.contactEmail">
                    {{ organization.contactEmail }}
                  </a>
                </li>
                <li>
                  <i class="bi bi-globe"></i>
                  <a :href="organization.uri" target="_blank">
                    {{ organization.uri }}
                  </a>
                </li>
              </ul>

              <!-- Resources Accordion -->
              <div class="accordion" :id="'accordionResources_' + organization.id">
                <!-- Toggle for Resources List -->
                <div class="accordion-item">
                  <h2 class="accordion-header" :id="'headingResources_' + organization.id">
                    <button
                      class="accordion-button"
                      type="button"
                      data-bs-toggle="collapse"
                      :data-bs-target="'#collapseResources_' + organization.id"
                      aria-expanded="false"
                      :aria-controls="'collapseResources_' + organization.id"
                      @click="loadingSpinner = true"
                    >
                      Resources
                    </button>
                  </h2>
                  <div
                    :id="'collapseResources_' + organization.id"
                    class="accordion-collapse collapse"
                    :aria-labelledby="'headingResources_' + organization.id"
                  >
                    <div class="accordion-body">
                      <!-- Individual Resources Accordion -->
                      <div
                        v-for="resource in organization.resources"
                        :key="resource.id"
                        class="accordion-item"
                      >
                        <h2 class="accordion-header" :id="'heading_' + resource.id">
                          <button
                            class="accordion-button collapsed"
                            type="button"
                            data-bs-toggle="collapse"
                            :data-bs-target="'#collapse_' + resource.id"
                            aria-expanded="false"
                            :aria-controls="'collapse_' + resource.id"
                            @click="loadingSpinner = true"
                          >
                            {{ resource.name }} ({{ resource.sourceId }})
                            <!-- Resource Status Icon -->
                            <i
                              v-if="resource.representatives.length > 0"
                              class="bi bi-check-circle-fill text-success"
                              title="This resource has representatives"
                            ></i>
                            <i
                              v-else
                              class="bi bi-exclamation-triangle-fill text-warning"
                              title="This resource has no representatives"
                            ></i>
                          </button>
                        </h2>

                        <!-- Resource Body -->
                        <div
                          :id="'collapse_' + resource.id"
                          class="accordion-collapse collapse"
                          :aria-labelledby="'heading_' + resource.id"
                        >
                          <div class="accordion-body" @transitionend="loadingSpinner = false">
                            <p>{{ resource.description }}</p>
                            <ul class="list-unstyled">
                              <li>
                                <i class="bi bi-envelope"></i>
                                <a :href="'mailto:' + resource.contactEmail">
                                  {{ resource.contactEmail }}
                                </a>
                              </li>
                              <li>
                                <i class="bi bi-globe"></i>
                                <a :href="resource.uri" target="_blank">
                                  {{ resource.uri }}
                                </a>
                              </li>
                            </ul>
                            <h6>Representatives:</h6>
                            <ul>
                              <li
                                v-for="rep in resource.representatives"
                                :key="rep"
                              >
                                <i class="bi bi-person"></i> {{ rep }}
                              </li>
                            </ul>
                            <!-- Warning if no representatives -->
                            <div v-if="resource.representatives.length === 0" class="text-warning mt-2">
                              <i class="bi bi-exclamation-triangle"></i> This resource has no representatives.
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>



      </div>
    </div>
  </div>
  <LoadingSpinner v-else: />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import moment from 'moment'
import { useNetworksPageStore } from '@/store/networksPage'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { useUserStore } from '@/store/user'
import externalLinks from '@/config/externalLinks'
import FilterSort from '@/components/FilterSort.vue'
import NegotiationList from '@/components/NegotiationList.vue'
import NegotiationPagination from '@/components/NegotiationPagination.vue'
import { useNegotiationsStore } from '@/store/negotiations'
import { Pie } from 'vue-chartjs'
import { ArcElement, CategoryScale, Chart as ChartJS, DoughnutController, Legend, Title, Tooltip } from 'chart.js'
import { generatePieChartBackgroundColorArray } from '../composables/utils.js'
import { useUiConfiguration } from '@/store/uiConfiguration.js'

ChartJS.register(Title, Tooltip, Legend, ArcElement, CategoryScale, DoughnutController)

// Pie chart data

const props = defineProps({
  networkId: {
    type: String,
    required: true,
  },
})

const uiConfigurationStore = useUiConfiguration()
const userStore = useUserStore()
const negotiationsStore = useNegotiationsStore()
const networksPageStore = useNetworksPageStore()
const network = ref(undefined)
const negotiations = ref(undefined)
const currentTab = ref('overview') // Default tab
const stats = ref(undefined)
const pagination = ref(undefined)
const states = ref(undefined)
const loadingSpinner = ref(false)
const filtersSortData = ref({
  status: [],
  dateStart: '',
  dateEnd: '',
  sortBy: 'creationDate',
  sortDirection: 'DESC',
})
// Helper function to check if all resources have representatives
const allResourcesHaveRepresentatives = (resources) => {
  return resources.every((resource) => resource.representatives.length > 0)
}
const totalResources = computed(() => {
  return organizations.value.reduce((sum, org) => sum + org.resources.length, 0)
})

const resourcesWithoutRepresentatives = computed(() => {
  return organizations.value.reduce(
    (sum, org) =>
      sum +
      org.resources.filter((resource) => resource.representatives.length === 0).length,
    0
  )
})

const today = new Date()
const startOfYear = new Date(today.getFullYear(), 0, 1)
const startDate = ref(startOfYear.toISOString().slice(0, 10))
const endDate = ref(today.toISOString().slice(0, 10))
const userRole = ref('author')
const isLoaded = ref(false)
const organizations = ref([])
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
})
const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})
onMounted(async () => {
  await userStore.retrieveUser()
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
  [network, states, stats],
  ([newNetwork, newStates, newStats]) => {
    isLoaded.value = !!(newNetwork && newStates && newStats)
  },
  { immediate: true }, // Run the watcher immediately on component mount
)
loadNetworkInfo(props.networkId)

async function loadOrganizations(networkId) {
  const response = await networksPageStore.retrieveNetworkOrganizations(networkId)
  if (response._embedded) {
    organizations.value = response._embedded.organizations
  }
}

loadOrganizations(props.networkId)
loadStats(props.networkId)
loadNegotiationStates()
retrieveLatestNegotiations(0)

async function loadNegotiationStates() {
  states.value = await negotiationsStore.retrieveNegotiationLifecycleStates()
}

async function loadNetworkInfo(networkId) {
  network.value = await networksPageStore.retrieveNetwork(networkId)
}

function formatStatusLabel(status) {
  // Convert status from snake case to title case for better display
  return status
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (char) => char.toUpperCase())
}
async function loadStats(networkId) {
  stats.value = await networksPageStore.retrieveNetworkStats(
    networkId,
    startDate.value,
    endDate.value,
  )

  if (stats.value.statusDistribution) {
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

.mt-3,
.mt-4 {
  margin-top: 1.5rem !important;
}

.row {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  width: 100%;
}

.btn {
  background-color: var(--button-color, #007bff); /* Default color if none provided */
  color: #ffffff;
  transition: background-color 0.3s ease, transform 0.2s ease;
  text-decoration: none; /* Prevent underline on links */
}

.btn:hover {
  background-color: var(--button-hover-color, #0056b3); /* Slightly darker color */
  transform: scale(1.05); /* Slight zoom-in effect */
}

.btn:active {
  transform: scale(1); /* Reset scale when clicked */
}

.col-md-6,
.col-lg-4 {
  flex: 1 1 45%; /* Adjust flex for responsiveness */
  max-width: 33%; /* Set max-width to prevent over-expansion */
}
</style>
