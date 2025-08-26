<template>
  <div class="mb-4">
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
          <input
            id="startDate"
            :value="startDate"
            type="date"
            class="form-control"
            @input="$emit('update:startDate', $event.target.value)"
          />
        </div>

        <div class="form-group d-inline-block mx-4">
          <label for="endDate" class="form-label">End Date:</label>
          <input
            id="endDate"
            :value="endDate"
            type="date"
            class="form-control"
            @input="$emit('update:endDate', $event.target.value)"
          />
        </div>
     
          <button
            type="button"
            class="btn btn-primary" 
            @click="setDateRange('sinceCurrentYear')"
          >
          Since current year
        </button>
        <button
            type="button"
            class="btn btn-primary" 
            @click="setDateRange('sinceOneYearAgo')"
          >
          Last year
        </button>
      
        
      </div>
    </div>

    <!-- Negotiations Card -->
    <div class="card">
      <div class="card-body">
        <!-- Card Header -->
        <div class="d-flex flex-row mb-2 align-items-center">
          <h4 class="card-title mb-0">Negotiations</h4>
          <i
            class="bi bi-info-circle ml-2 small-icon"
            title="States of different Negotiations involving Resources in this Network"
          />
        </div>

        <!-- Total Number of Requests -->
        <div class="text-center mb-2">
          <h5>Total Negotiations: {{ stats.totalNumberOfNegotiations }}</h5>
        </div>

        <!-- Pie Chart Section -->
        <div v-if="stats" class="pie-chart-container">
          <Pie :data="pieData" :options="pieOptions" />
        </div>
      </div>
    </div>

    <!-- Status Distribution Card -->
    <div class="card mt-4">
      <div class="card-body">
        <div class="d-flex flex-row mb-4 align-items-center">
          <h4 class="card-title mb-0">Status Distribution</h4>
          <i
            class="bi bi-info-circle ml-2 small-icon"
            title="Statistics showing the distribution of negotiation statuses"
          />
        </div>

        <div class="row mt-4">
          <div
            v-for="(count, status) in stats.statusDistribution"
            :key="status"
            class="col-md-6 col-lg-4 mb-4 d-flex"
          >
            <NetworkStatsCard
              :label="formatStatusLabel(status)"
              :value="count"
              :tooltip="'The number of negotiations with status: ' + formatStatusLabel(status)"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Additional Information Card -->
    <div class="card mt-4">
      <div class="card-body">
        <div class="d-flex flex-row mb-4 align-items-center">
          <h4 class="card-title mb-0">Additional Information</h4>
          <i
            class="bi bi-info-circle ml-2 small-icon"
            title="Additional statistics related to negotiations"
          />
        </div>

        <div class="row mt-4">
          <div class="col-md-6 col-lg-4 mb-4 d-flex">
            <NetworkStatsCard
              label="Ignored Negotiations"
              :value="stats.numberOfIgnoredNegotiations"
              tooltip="Negotiations which involved at least one Resource in your Network and none responded"
              :clickable="true"
              @click="$emit('setNegotiationIds', stats.negotiationIds['Ignored'])"
            />
          </div>

          <div class="col-md-6 col-lg-4 mb-4 d-flex">
            <NetworkStatsCard
              label="Median Response Time in Days"
              :value="stats.medianResponseTime || 'N/A'"
              tooltip="Median number of days taken for negotiations to receive responses"
              :muted="true"
            />
          </div>

          <div class="col-md-6 col-lg-4 mb-4 d-flex">
            <NetworkStatsCard
              label="Successful Negotiations"
              :value="stats.numberOfSuccessfulNegotiations"
              tooltip="The number of negotiations where at least one Resource in you Network has been provided to the Requester"
              :clickable="true"
              @click="$emit('setNegotiationIds', stats.negotiationIds['Successful'])"
            />
          </div>

          <div class="col-md-6 col-lg-4 mb-4 d-flex">
            <NetworkStatsCard
              label="New Requesters"
              :value="stats.numberOfNewRequesters"
              tooltip="The number of new requesters in this network"
            />
          </div>

          <div class="col-md-6 col-lg-4 mb-4 d-flex">
            <NetworkStatsCard
              label="Active Representatives"
              :value="stats.numberOfActiveRepresentatives"
              tooltip="The number of active representatives currently in the network"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Pie } from 'vue-chartjs'
import NetworkStatsCard from './NetworkStatsCard.vue'

defineProps({
  stats: {
    type: Object,
    required: true,
  },
  startDate: {
    type: String,
    required: true,
  },
  endDate: {
    type: String,
    required: true,
  },
  pieData: {
    type: Object,
    required: true,
  },
  pieOptions: {
    type: Object,
    required: true,
  },
})

const emits = defineEmits(['update:startDate', 'update:endDate', 'setNegotiationIds'])

function formatStatusLabel(status) {
  return status
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (char) => char.toUpperCase())
}

function setDateRange(range) {
  const formatDate = (date) => date.toISOString().slice(0, 10)

  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(today.getDate() - 1)

  let startDate

  switch (range) {
    case 'sinceCurrentYear':
      startDate = new Date(today.getFullYear(), 0, 1) 
      break

    case 'sinceOneYearAgo':
      startDate = new Date(today)
      startDate.setFullYear(today.getFullYear() - 1) 
      break

    default:
      console.warn(`Unknown range: ${range}`)
      startDate = today
  }

  emits('update:startDate', formatDate(startDate))
  emits('update:endDate', formatDate(yesterday))
}
</script>

<style scoped>
.pie-chart-container {
  width: 100%;
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: auto;
}

.card {
  border-radius: 10px;
  border: 1px solid #dee2e6;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  background-color: #fff;
  width: 100%;
  box-sizing: border-box;
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

.small-icon {
  font-size: 0.75rem;
}

.d-flex {
  display: flex;
  flex-direction: row;
  gap: 1rem;
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
  margin-left: 0;
  margin-right: 0;
}

.col-md-6,
.col-lg-4 {
  flex: 1 1 45%;
  max-width: 33%;
}
</style>
