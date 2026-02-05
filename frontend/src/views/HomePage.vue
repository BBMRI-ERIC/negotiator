<template>
  <div class="home-page">
    <p class="app-name-text my-0 fw-bold">NEGOTIATOR</p>
    <p>
      The BBMRI-ERIC Negotiator is a service that provides an efficient communication platform for
      biobankers and researchers requesting samples and/or data.
      <br />

      The Negotiator is connected to the already established BBMRI-ERIC Directory, the biggest
      biobanking catalogue on the globe.
    </p>

    <div class="navigation-panel d-flex flex-row flex-wrap my-4">
      <BigButton
        :buttonIcon="'bi-building'"
        :buttonText="'Documentation'"
        @click="openDocumentation()"
      />
      <BigButton
        :buttonIcon="'bi-people'"
        :buttonText="$t('navbar.FAQ')"
        @click="$router.push('/FAQ')"
      />
      <BigButton
        :buttonIcon="'bi-play-circle'"
        :buttonText="'Take a Tour'"
        @click="$router.push('/guide')"
      />
    </div>
  </div>
  <div class="row">
    <div v-if="userRoles.includes(ROLES.ADMINISTRATOR)" class="mb-5 col-12 col-md-6">
      <h2 class="my-0 fw-bold mb-3 text-uppercase">{{ $t('navbar.admin') }}</h2>
      <UserPage
        :userRole="ROLES.ADMINISTRATOR"
        :isHomePage="true"
        :totalNegotiationsCount="totalNegotiationsCount"
      />
      <div class="d-flex justify-content-center mt-2 w-100">
        <PrimaryButton @click="$router.push('/admin')"> see more </PrimaryButton>
      </div>
    </div>
    <div v-if="userRoles.includes(ROLES.RESEARCHER)" class="mb-5 col-12 col-md-6">
      <h2 class="my-0 fw-bold mb-3 text-uppercase">{{ $t('navbar.researcher') }}</h2>
      <UserPage
        :userRole="ROLES.RESEARCHER"
        :isHomePage="true"
        :totalNegotiationsCount="totalNegotiationsCount"
      />
      <div class="d-flex justify-content-center mt-2 w-100">
        <PrimaryButton @click="$router.push('/researcher')"> see more </PrimaryButton>
      </div>
    </div>
    <div v-if="userRoles.includes(ROLES.REPRESENTATIVE)" class="mb-5 col-12 col-md-6">
      <h2 class="my-0 fw-bold mb-3 text-uppercase">{{ $t('navbar.biobanker') }}</h2>
      <UserPage
        :userRole="ROLES.REPRESENTATIVE"
        :isHomePage="true"
        :totalNegotiationsCount="totalNegotiationsCount"
      />
      <div class="d-flex justify-content-center mt-2 w-100">
        <PrimaryButton @click="$router.push('/biobanker')"> see more </PrimaryButton>
      </div>
    </div>
  </div>
  <div class="discovery-services mb-5">
    <h2 class="my-0 fw-bold mb-3">Discovery Services</h2>
    <div class="d-flex flex-row flex-wrap">
      <div v-for="(service, index) in allDiscoveryServices" :key="index">
        <BigButton
          :buttonIcon="'bi-database'"
          :buttonText="`${service.name}`"
          @click="openService(service.url)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import UserPage from './UserPage.vue'
import { useUserStore } from '../store/user.js'
import { ROLES } from '@/config/consts.js'
import BigButton from '@/components/ui/buttons/BigButton.vue'
import { useDiscoveryServicesStore } from '../store/discoveryServices.js'
import PrimaryButton from '@/components/ui/buttons/PrimaryButton.vue'

const userStore = useUserStore()
const discoveryServices = useDiscoveryServicesStore()
const userRoles = ref([])

const allDiscoveryServices = ref([])
const INITIAL_TOTAL_NEGOTIATIONS_COUNT = 3
const totalNegotiationsCount = ref(INITIAL_TOTAL_NEGOTIATIONS_COUNT)

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
  userRoles.value = userStore.userInfo.roles

  allDiscoveryServices.value = await discoveryServices.retrieveDiscoveryServices()
})

function openDocumentation() {
  window.open('https://bbmri-eric.github.io/negotiator/requester', '_blank', 'noopener')
}

function openService(service) {
  window.open(service, '_blank', 'noopener')
}
</script>

<style scoped>
.home-page {
  margin-top: -20px;
}

.app-name-text {
  font-size: 4rem;
}

@media only screen and (min-width: 768px) {
  .app-name-text {
    font-size: 6rem;
  }
}
</style>
