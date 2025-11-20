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

    <div class="navigation-panel d-flex flex-row my-3">
      <button
        class="btn panel-button d-flex flex-row align-items-center rounded-4 me-4 my-4 px-4"
        @click="openDocumentation()"
      >
        <h4 class="my-1"><i class="bi bi-building me-2 my-0"></i></h4>
        <h4 class="my-0 fw-bold">Documentation</h4>
      </button>
      <button
        class="btn panel-button d-flex flex-row align-items-center rounded-4 me-4 my-4 px-4"
        @click="$router.push('/guide')"
      >
        <h4 class="my-1"><i class="bi bi-play-circle me-2 my-0"></i></h4>
        <h4 class="my-0 fw-bold">Take a Tour</h4>
      </button>
      <button
        class="btn panel-button d-flex flex-row align-items-center rounded-4 me-4 my-4 px-4"
        @click="openDirectory()"
      >
        <h4 class="my-1"><i class="bi bi-database me-2 my-0"></i></h4>
        <h4 class="my-0 fw-bold">Back to Directory</h4>
      </button>
    </div>
  </div>

  <div v-if="userRoles.includes(ROLES.RESEARCHER)" class="mb-5">
    <h2 class="my-0 fw-bold mb-3">Yours Negotiations</h2>

    <UserPage
      v-if="userRoles.includes(ROLES.REPRESENTATIVE)"
      :userRole="'ROLE_RESEARCHER'"
      :isHomePage="true"
    />
  </div>
  <div class="mb-5">
    <h2 class="my-0 fw-bold mb-3">REPRESENTATIVE REQUESTS</h2>

    <UserPage :userRole="'ROLE_REPRESENTATIVE'" :isHomePage="true" />
  </div>

  <div v-if="userRoles.includes(ROLES.ADMINISTRATOR)" class="mb-5">
    <h2 class="my-0 fw-bold mb-3">REVIEW REQUESTS</h2>

    <UserPage :userRole="'ROLE_ADMIN'" :isHomePage="true" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import UserPage from './UserPage.vue'
import { useUserStore } from '../store/user.js'
import { ROLES } from '@/config/consts.js'

const userStore = useUserStore()
const userRoles = ref(userStore.userInfo.roles)

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})

function openDocumentation() {
  window.open('https://bbmri-eric.github.io/negotiator/requester', '_blank')
}

function openDirectory() {
  window.open('https://directory.bbmri-eric.eu', '_blank')
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

.panel-button {
  background-color: #f8f8f8;
}
.panel-button:hover,
.panel-button:focus {
  background: #3d3d3d;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  outline: none;
}
</style>
