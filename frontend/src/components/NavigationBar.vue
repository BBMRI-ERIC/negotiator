<template>
  <nav
    v-if="oidcIsAuthenticated"
    id="v-step-0"
    class="navbar fixed-top navbar-expand-lg"
    :style="{ 'background-color': uiConfiguration?.navbarBackgroundColor }"
  >
    <div class="container-fluid">
      <router-link to="/">
        <img :src="returnLogoSrc" height="34" class="me-2" alt="nav-bar-logo" />
      </router-link>
      <div id="menu-navbar" class="collapse navbar-collapse">
        <ul class="navbar-nav me-auto my-2 my-lg-0 navbar-nav-scroll">
          <li v-if="isAdmin" class="nav-item">
            <router-link
              class="nav-link active nav-option"
              :style="{
                color:
                  $route.path === '/admin' || $route.params.userRole === 'ROLE_ADMIN'
                    ? uiConfiguration?.navbarActiveTextColor
                    : uiConfiguration?.navbarTextColor,
              }"
              to="/admin"
            >
              <i class="bi bi-clipboard-check" />
              Review requests
            </router-link>
          </li>
          <li v-if="isResearcher" class="nav-item">
            <router-link
              class="nav-link active nav-option"
              :style="{
                color:
                  $route.path === '/researcher' || $route.params.userRole === 'ROLE_RESEARCHER'
                    ? uiConfiguration?.navbarActiveTextColor
                    : uiConfiguration?.navbarTextColor,
              }"
              to="/researcher"
            >
              <i class="bi bi-chat-left-dots" />
              Your negotiations
            </router-link>
          </li>
          <li v-if="isRepresentative" class="nav-item">
            <router-link
              class="nav-link active nav-option"
              :style="{
                color:
                  $route.path === '/biobanker' || $route.params.userRole === 'ROLE_REPRESENTATIVE'
                    ? uiConfiguration?.navbarActiveTextColor
                    : uiConfiguration?.navbarTextColor,
              }"
              to="/biobanker"
            >
              <i class="bi bi-bank" />
              Your biobank
            </router-link>
          </li>
          <li v-if="showNetworksTab" class="nav-item dropdown" :class="{ show: dropdownVisible }">
            <a
              id="networksDropdown"
              class="nav-link active nav-option dropdown-toggle"
              :style="{
                color: $route.path.startsWith('/networks')
                  ? uiConfiguration?.navbarActiveTextColor
                  : uiConfiguration?.navbarTextColor,
              }"
              href="#"
              role="button"
              @click="toggleDropdown"
            >
              <i class="bi bi-globe" />
              Your networks
            </a>
            <ul class="dropdown-menu dropdown-menu-right" :class="{ show: dropdownVisible }">
              <li v-for="network in networks" :key="network.id">
                <a class="dropdown-item" href="#" @click="selectNetwork(network.id)">
                  {{ network.name }}
                </a>
              </li>
            </ul>
          </li>
          <li v-if="featureFlagsFAQ" class="nav-item">
            <router-link
              class="nav-link active nav-option"
              :style="{
                color:
                  $route.path === '/FAQ'
                    ? uiConfiguration?.navbarActiveTextColor
                    : uiConfiguration?.navbarTextColor,
              }"
              to="/FAQ"
            >
              <i class="bi bi-people" />
              Support
            </router-link>
          </li>
        </ul>
        <div
          v-if="oidcIsAuthenticated && returnCurrentMode"
          class="me-2 me-3"
          :class="returnCurrentModeTextColor"
        >
          <div class="spinner-grow spinner-grow-sm" role="status" />
          {{ returnCurrentMode }}
        </div>
        <NotificationsButton v-if="featureFlagsNotifications" class="me-3" />
        <span
          v-if="oidcIsAuthenticated"
          class="me-2"
          :style="{ color: uiConfiguration?.navbarWelcomeTextColor }"
        >
          {{ oidcUser.preferred_username }}
        </span>
      </div>
      <div>
        <ProfileSettings
          :user="oidcUser"
          :is-representative="isRepresentative"
          :is-admin="isAdmin"
          class="me-3"
        />
        <button
          class="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#menu-navbar"
          aria-controls="menu-navbar"
          aria-expanded="false"
        >
          <span class="navbar-toggler-icon" />
        </button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, onBeforeMount, ref, watch } from 'vue'
import { ROLES } from '@/config/consts'
import ProfileSettings from '../components/ProfileSettings.vue'
import bbmriLogo from '../assets/images/bbmri/nav-bar-bbmri.png'
import canservLogo from '../assets/images/canserv/nav-bar-canserv.png'
import eucaimLogo from '../assets/images/eucaim/nav-bar-eucaim.png'
import NotificationsButton from './NotificationsButton.vue'
import allFeatureFlags from '@/config/featureFlags.js'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import { useActuatorInfoStore } from '../store/actuatorInfo'
import { useUserStore } from '../store/user'
import { useOidcStore } from '../store/oidc'
import { useNetworksPageStore } from '../store/networksPage'
import { useRouter } from 'vue-router'

const uiConfigurationStore = useUiConfiguration()
const actuatorInfoStore = useActuatorInfoStore()
const userStore = useUserStore()
const oidcStore = useOidcStore()
const networksPageStore = useNetworksPageStore()
const dropdownVisible = ref(false)
const router = useRouter()
const roles = ref([])
const featureFlagsFAQ = !!(allFeatureFlags.faqPage === 'true' || allFeatureFlags.faqPage === true)
const featureFlagsNotifications = !!(
  allFeatureFlags.notifications === 'true' || allFeatureFlags.notifications === true
)
const backendEnvironment = ref('')
const showNetworksTab = ref(false)
const networks = ref([])
const selectNetwork = (networkId) => {
  toggleDropdown()
  router.push(`/networks/${networkId}`)
}

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.navbar
})
const oidcIsAuthenticated = computed(() => {
  return oidcStore.oidcIsAuthenticated
})
const oidcUser = computed(() => {
  return oidcStore.oidcUser
})
const isAdmin = computed(() => {
  return roles.value.includes(ROLES.ADMINISTRATOR)
})
const isResearcher = computed(() => {
  return roles.value.includes(ROLES.RESEARCHER)
})
const isRepresentative = computed(() => {
  return roles.value.includes(ROLES.REPRESENTATIVE)
})
const returnCurrentMode = computed(() => {
  if (import.meta.env.DEV) {
    return 'Development Server'
  } else if (backendEnvironment.value === 'Acceptance') {
    return 'Acceptance Server'
  }
  return ''
})
const returnCurrentModeTextColor = computed(() => {
  if (import.meta.env.DEV) {
    return 'text-success'
  } else if (backendEnvironment.value === 'Acceptance') {
    return 'text-warning'
  }
  return ''
})
const userInfo = computed(() => {
  return userStore.userInfo
})
const returnLogoSrc = computed(() => {
  if (uiConfiguration.value?.navbarLogoUrl === 'bbmri') {
    return bbmriLogo
  } else if (uiConfiguration.value?.navbarLogoUrl === 'canserv') {
    return canservLogo
  } else if (uiConfiguration.value?.navbarLogoUrl === 'eucaim') {
    return eucaimLogo
  }
  return uiConfiguration.value?.navbarLogoUrl
})
const toggleDropdown = () => {
  dropdownVisible.value = !dropdownVisible.value
}

async function retrieveUserNetworks() {
  networks.value = await networksPageStore.retrieveUserNetworks(userInfo.value.id)
}

watch(userInfo, () => {
  retrieveUserRoles()
  if (userInfo.value._links.networks !== undefined) {
    showNetworksTab.value = true
    retrieveUserNetworks()
  }
})
onBeforeMount(() => {
  actuatorInfoStore.retrieveBackendActuatorInfo().then(() => {
    retrieveBackendEnvironment()
  })
})

function retrieveBackendEnvironment() {
  backendEnvironment.value = actuatorInfoStore.actuatorInfoApplicationEnvironment
}

function retrieveUserRoles() {
  roles.value = userInfo.value.roles
}
</script>

<style>
nav {
  width: 100%;
  font-size: 1rem;
  text-align: left;
}
.nav-item.dropdown .dropdown-menu {
  min-width: 140px; /* Set the minimum width of the dropdown */
  max-width: 200px; /* Ensure it doesn't exceed the width of the navbar item */
  background-color: #e7e7e7; /* Light gray background to match the Bootstrap light navbar */
  border: 1px solid #dee2e6; /* Light border for the dropdown */
  border-radius: 0; /* No border-radius for a flush fit with the navbar */
  box-shadow: none; /* Remove shadow for a flat appearance */
}

.nav-item.dropdown .dropdown-item {
  white-space: nowrap; /* Prevent text from wrapping */
  overflow: hidden;
  text-overflow: ellipsis; /* Ellipsis for overflowing text */
  color: #495057; /* Darker gray text color to match Bootstrap's default text */
  background-color: #e7e7e7;
}
.nav-item:hover .nav-link,
.nav-item.dropdown .dropdown-item:hover,
.nav-item.dropdown .dropdown-item:focus {
  box-shadow: inset 0 0 100px 100px rgba(255, 255, 255, 0.3); /* Light gray background on hover */
}
</style>
