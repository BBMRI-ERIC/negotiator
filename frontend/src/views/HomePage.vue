<template>
  <div class="home-page">
    <p class="app-name-text my-0">NEGOTIATOR</p>
    <p>
      The BBMRI-ERIC Negotiator is service that provides an efficient communication platform for
      biobankers and researchers requesting samples and/or data.
      <br />

      The Negotiator is connected to the already established BBMRI-ERIC Directory, the biggest
      biobanking catalogue on the globe.
    </p>

    <div class="navigation-panel d-flex flex-row flex-wrap my-4">
      <BigHomeButton
        :buttonIcon="'bi-building'"
        :buttonText="'Documentation'"
        @click="openDocumentation()"
      />
      <BigHomeButton
        :buttonIcon="'bi-play-circle'"
        :buttonText="'Take a Tour'"
        @click="$router.push('/guide')"
      />
      <div class="discovery-services" v-for="(service, index) in allDiscoveryServices" :key="index">
        <BigHomeButton
          :buttonIcon="'bi-database'"
          :buttonText="`${service.name}`"
          @click="openService(service.url)"
        />
      </div>
    </div>
  </div>
  <div class="d-flex">
    <div v-if="userRoles.includes(ROLES.RESEARCHER)" class="mb-5">
      <h2 class="my-0 fw-bold mb-3">YOURS NEGOTIATIONS</h2>
      <UserPage :userRole="'ROLE_RESEARCHER'" :isHomePage="true" />
    </div>
    <div v-if="userRoles.includes(ROLES.REPRESENTATIVE)" class="mb-5">
      <h2 class="my-0 fw-bold mb-3">REPRESENTATIVE REQUESTS</h2>
      <UserPage :userRole="'ROLE_REPRESENTATIVE'" :isHomePage="true" />
    </div>

    <div v-if="userRoles.includes(ROLES.ADMINISTRATOR)" class="mb-5">
      <h2 class="my-0 fw-bold mb-3">REVIEW REQUESTS</h2>
      <UserPage :userRole="'ROLE_ADMIN'" :isHomePage="true" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import UserPage from './UserPage.vue'
import { useUserStore } from '../store/user.js'
import { ROLES } from '@/config/consts.js'
import BigHomeButton from '@/components/BigHomeButton.vue'
import { useDiscoveryServicesStore } from '../store/discoveryServices.js'

const userStore = useUserStore()
const discoveryServices = useDiscoveryServicesStore()
const userRoles = ref(userStore.userInfo.roles)

const allDiscoveryServices = ref([])

onMounted(async () => {
  allDiscoveryServices.value = await discoveryServices.retrieveDiscoveryServices()
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})

function openDocumentation() {
  window.open('https://bbmri-eric.github.io/negotiator/requester', '_blank')
}

function openService(service) {
  window.open(service, '_blank')
}
</script>

<style scoped>
.home-page {
  margin-top: -20px;
}
.app-name-text {
  font-size: 6rem;
  font-weight: bold;
}
.card {
  box-shadow:
    0 3px 6px 0 rgba(0, 0, 0, 0.2),
    0 3px 6px 0 rgba(0, 0, 0, 0.19);
}
</style>
