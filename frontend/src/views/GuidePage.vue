<template>
  <div class="guide-page">
    <h1>Welcome to the Negotiator Guide</h1>
    <p>This guide will walk you through how to use the app effectively from a user perspective.</p>

    <section>
      <h2>Key Features</h2>
      <ul>
        <li>
          <strong>Menu:</strong> Navigating the BBMRI-ERIC Negotiator

          <p class="mb-0">
            The navigation bar at the top of the screen is your primary tool for getting around the
            platform.
            <button @click="startNavTour()" class="btn btn-sm btn-outline-dark">
              Take the Tour
            </button>
          </p>
        </li>
        <li>
          <strong>Filter:</strong> Use the filter bar to quickly find what you're looking for.
          <button @click="startFilterSortTour()" class="btn btn-sm btn-outline-dark my-3">
            Take the Tour
          </button>
        </li>
        <li>
          <strong>Negotiation Page:</strong> It allows users to manage and track the progress of
          negotiations efficiently   
          <button @click="startNegotiationTour()" class="btn btn-sm btn-outline-dark my-3">
              Take the Tour
            </button>
        </li>
      </ul>
    </section>

    <section>
      <h2>Access Documentation</h2>
      <p>Learn more about the Negotiator by visiting the official documentation.</p>
      <a
        href="https://bbmri-eric.github.io/negotiator/what-is-negotiator"
        target="_blank"
        class="doc-link"
      >
        <i class="bi bi-book"></i> Documentation
      </a>
    </section>
  </div>
</template>

<script setup>
import { useVueTourStore } from '../store/vueTour'
import { useRouter } from 'vue-router'
import { useNotificationsStore } from '../store/notifications.js'

const notificationsStore = useNotificationsStore()
const vueTourStore = useVueTourStore()
const router = useRouter()

function startNavTour() {
  showNotification(
    'Starting the Navigation Tour! Follow the prompts to learn about the navigation features.',
    'info',
  )
  vueTourStore.isNavTourActive = true
}
function startFilterSortTour() {
  showNotification(
    'Starting the Filter and Sort Tour! Follow the prompts to learn about filtering and sorting features.',
    'info',
  )
  vueTourStore.isFilterSortTourActive = true
  vueTourStore.isFilterSortVisible = false
  router.push('/')
}

function startNegotiationTour() {
  showNotification(
    'Starting the Negotiation Tour! Please select a negotiation from the list to begin the tour.',
    'info',
  )

  vueTourStore.isNegotiationTourActive = true
  vueTourStore.isNegotiationVisible = false

  router.push('/')
}

function showNotification(message, type) {
  notificationsStore.setNotification(message, type)
}
</script>

<style scoped>
.guide-page {
  font-family: Arial, sans-serif;
  line-height: 1.6;
  padding: 20px;
}

h1 {
  color: #2c3e50;
}

h2 {
  color: #34495e;
  margin-top: 20px;
}

ul {
  list-style-type: disc;
  margin-left: 20px;
}

p {
  margin-bottom: 15px;
}
</style>
