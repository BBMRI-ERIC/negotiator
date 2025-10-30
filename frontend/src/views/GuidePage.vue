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
        <li v-if="isGovernanceVisible">
          <strong>Governance:</strong> The Negotiator operates on a hierarchical governance
          structure designed to mirror real-world organizational relationships and resource
          management. Understanding this structure is crucial for proper system administration and
          ensuring smooth negotiation workflows
          <button @click="startGovernanceTour()" class="btn btn-sm btn-outline-dark my-3">
            Take the Tour
          </button>
        </li>
      </ul>
    </section>

    <section>
      <h2>Documentation</h2>
      <p>
        Learn more about the Negotiator by visiting the official<a
          href="https://bbmri-eric.github.io/negotiator/what-is-negotiator"
          target="_blank"
          class="doc-link"
        >
          Documentation
        </a>
      </p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user.js'
import { useVueTourStore } from '../store/vueTour'
import { useNotificationsStore } from '../store/notifications.js'
import { ROLES } from '@/config/consts'

const router = useRouter()
const userStore = useUserStore()
const notificationsStore = useNotificationsStore()
const vueTourStore = useVueTourStore()

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})

const isGovernanceVisible = computed(() => {
  return (
    userStore.userInfo.roles.includes(ROLES.REPRESENTATIVE) ||
    userStore.userInfo.roles.includes(ROLES.ADMINISTRATOR)
  )
})

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

function startGovernanceTour() {
  showNotification('Starting the Governance Tour!', 'info')

  vueTourStore.isGovernanceTourActive = true
  router.push('/governance')
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
