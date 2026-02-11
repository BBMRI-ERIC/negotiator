<template>
  <div class="guide-page">
    <h1>Welcome to the Negotiator Guide</h1>
    <p>This guide will walk you through how to use the app effectively from a user perspective.</p>

    <section>
      <h3>Key Features</h3>
      <ul>
        <li>
          <strong>Menu:</strong> Navigating the BBMRI-ERIC Negotiator
          <PrimaryButton @click="startNavTour()" size="sm" class="ms-2"
            >Take the Tour</PrimaryButton
          >
        </li>
        <li>
          <strong>Filter:</strong> Use the filter bar to quickly find what you're looking for.
          <PrimaryButton @click="startFilterSortTour()" size="sm" class="ms-2"
            >Take the Tour</PrimaryButton
          >
        </li>
        <li>
          <strong>Negotiation Page:</strong> It allows users to manage and track the progress of
          negotiations efficiently
          <PrimaryButton @click="startNegotiationTour()" size="sm" class="ms-2"
            >Take the Tour</PrimaryButton
          >
        </li>
        <li v-if="isRepresentative || isAdmin">
          <strong>Governance:</strong> The Negotiator operates on a hierarchical governance
          structure designed to mirror real-world organizational relationships and resource
          management.
          <PrimaryButton @click="startGovernanceTour()" size="sm" class="ms-2"
            >Take the Tour</PrimaryButton
          >
        </li>
        <li v-if="isAdmin">
          <strong>Admin Settings:</strong> Manage Information Requirements, Webhooks, Emails, Users,
          Templates, Access Forms and Form Elements.
          <PrimaryButton @click="startAdminSettingsTour()" size="sm" class="ms-2"
            >Take the Tour</PrimaryButton
          >
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
import PrimaryButton from '@/components/ui/buttons/PrimaryButton.vue'
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
  console.log('GuidePage mounted', userStore.userInfo)

  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})

const isRepresentative = computed(() => {
  return userStore.userInfo.roles?.includes(ROLES.REPRESENTATIVE)
})

const isAdmin = computed(() => {
  return userStore.userInfo.roles?.includes(ROLES.ADMINISTRATOR)
})

function startNavTour() {
  showNotification(
    'Starting the Navigation Tour! Follow the prompts to learn about the navigation features.',
    'info',
  )
  disableOtherTours()
  vueTourStore.isNavTourActive = true
}
function startFilterSortTour() {
  showNotification(
    'Starting the Filter and Sort Tour! Follow the prompts to learn about filtering and sorting features.',
    'info',
  )
  disableOtherTours()
  vueTourStore.isFilterSortTourActive = true
  vueTourStore.isFilterSortVisible = false
  router.push('/')
}

function startNegotiationTour() {
  showNotification(
    'Starting the Negotiation Tour! Please select a negotiation from the list to begin the tour.',
    'info',
  )
  disableOtherTours()
  vueTourStore.isNegotiationTourActive = true
  vueTourStore.isNegotiationVisible = false

  router.push('/')
}

function startGovernanceTour() {
  showNotification('Starting the Governance Tour!', 'info')
  disableOtherTours()
  vueTourStore.isGovernanceTourActive = true
  router.push('/governance')
}

function startAdminSettingsTour() {
  showNotification('Starting the Settings Tour!', 'info')
  disableOtherTours()
  vueTourStore.isAdminSettingsTourActive = true
  vueTourStore.isSettingsVisible = false
  router.push('/settings')
}

function showNotification(message, type) {
  notificationsStore.setNotification(message, type)
}

function disableOtherTours() {
  vueTourStore.isDefaultTourActive = false
  vueTourStore.isNavTourActive = false
  vueTourStore.isFilterSortTourActive = false
  vueTourStore.isNegotiationTourActive = false
  vueTourStore.isGovernanceTourActive = false
  vueTourStore.isAdminSettingsTourActive = false
}
</script>

<style scoped>
li {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
}

h1 {
  color: #2c3e50;
}
</style>
