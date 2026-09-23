<template>
  <nav
    v-if="oidcIsAuthenticated"
    id="v-step-0"
    :style="{ 'background-color': uiConfiguration?.navbarBackgroundColor }"
    class="navbar fixed-top navbar-expand-lg"
  >
    <div class="container-fluid px-4 px-md-5">
      <router-link class="navbar-logo" to="/">
        <img :src="returnLogoSrc" alt="nav-bar-logo" class="me-5" height="28" />
      </router-link>
      <div id="menu-navbar" ref="menuNavbarRef" class="collapse navbar-collapse">
        <ul class="navbar-nav me-auto my-2 my-lg-0 navbar-nav-scroll">
          <li v-if="isAdmin" class="nav-item v-step-10">
            <router-link
              :class="['nav-link nav-option', { active: isAdminActive }]"
              :style="navLinkStyle(isAdminActive)"
              to="/admin"
            >
              <i class="bi bi-clipboard-check" />
              {{ $t('navbar.admin') }}
            </router-link>
          </li>
          <li v-if="isResearcher" class="nav-item v-step-11">
            <router-link
              :class="['nav-link nav-option', { active: isResearcherActive }]"
              :style="navLinkStyle(isResearcherActive)"
              to="/researcher"
            >
              <i class="bi bi-chat-left-dots" />
              {{ $t('navbar.researcher') }}
            </router-link>
          </li>
          <li v-if="isRepresentative" class="nav-item v-step-12">
            <router-link
              :class="['nav-link nav-option', { active: isRepresentativeActive }]"
              :style="navLinkStyle(isRepresentativeActive)"
              to="/biobanker"
            >
              <i class="bi bi-bank" />
              {{ $t('navbar.biobanker') }}
            </router-link>
          </li>
          <li v-if="isRepresentative || isAdmin" class="nav-item v-step-13">
            <router-link
              :class="['nav-link nav-option', { active: isGovernanceActive }]"
              :style="navLinkStyle(isGovernanceActive)"
              to="/governance"
            >
              <i class="bi bi-archive" />
              Governance
            </router-link>
          </li>
          <!-- Dropdown for multiple networks -->
          <li
            v-if="showNetworksTab && networks.length > 1"
            :class="{ show: dropdownVisible }"
            class="nav-item dropdown v-step-14"
          >
            <a
              id="networksDropdown"
              :class="['nav-link nav-option dropdown-toggle', { active: isNetworksActive }]"
              :style="navLinkStyle(isNetworksActive)"
              href="#"
              role="button"
              @click="toggleDropdown"
            >
              <i class="bi bi-globe"></i>
              Your networks
            </a>
            <ul :class="{ show: dropdownVisible }" class="dropdown-menu dropdown-menu-right">
              <li v-for="network in networks" :key="network.id">
                <a class="dropdown-item" href="#" @click="selectNetwork(network.id)">
                  {{ network.name }}
                </a>
              </li>
            </ul>
          </li>

          <!-- Single network display as clickable -->
          <li v-else-if="showNetworksTab && networks.length === 1" class="nav-item v-step-14">
            <a
              :class="['nav-link nav-option', { active: isNetworksActive }]"
              :style="navLinkStyle(isNetworksActive)"
              href="#"
              @click="selectNetwork(networks[0].id)"
            >
              <i class="bi bi-globe"></i>
              {{ networks[0].name }}
            </a>
          </li>

          <li v-if="featureFlagsFAQ" class="nav-item v-step-15">
            <router-link
              :class="['nav-link nav-option', { active: isFaqActive }]"
              :style="navLinkStyle(isFaqActive)"
              to="/FAQ"
            >
              <i class="bi bi-people" />
              {{ $t('navbar.FAQ') }}
            </router-link>
          </li>
          <li class="nav-item">
            <router-link
              :class="['nav-link nav-option', { active: isGuideActive }]"
              :style="navLinkStyle(isGuideActive)"
              to="/guide"
            >
              <i class="bi bi-book" />
              {{ $t('navbar.guide') }}
            </router-link>
          </li>
        </ul>
      </div>
      <div
        v-if="oidcIsAuthenticated && returnCurrentMode"
        :class="returnCurrentModeTextColor"
        :title="returnCurrentMode"
        class="me-3 d-inline-flex align-items-center gap-2 navbar-env-indicator"
      >
        <div class="spinner-grow spinner-grow-sm" role="status" />
        <span class="d-none d-lg-inline text-nowrap fw-semibold">{{ returnCurrentMode }}</span>
      </div>
      <NotificationsButton class="me-3 v-step-16 navbar-notifications" />

      <button
        ref="togglerRef"
        aria-controls="menu-navbar"
        aria-expanded="false"
        class="navbar-toggler navbar-toggler-mobile-left border-0 shadow-none"
        data-bs-target="#menu-navbar"
        data-bs-toggle="collapse"
        type="button"
      >
        <span class="navbar-toggler-icon" />
      </button>

      <ProfileSettings
        :is-admin="isAdmin"
        :is-representative="isRepresentative"
        :user="oidcUser"
        class="v-step-17 navbar-profile"
      />
    </div>
  </nav>
</template>

<script setup>
import { computed, onBeforeMount, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Collapse } from 'bootstrap'
import { ROLES } from '@/config/consts'
import ProfileSettings from '../components/ProfileSettings.vue'
import bbmriLogo from '../assets/images/bbmri/nav-bar-bbmri.png'
import canservLogo from '../assets/images/canserv/nav-bar-canserv.png'
import eucaimLogo from '../assets/images/eucaim/nav-bar-eucaim.png'
import NotificationsButton from './NotificationsButton.vue'
import { useFeatureFlags } from '@/composables/useFeatureFlags.js'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import { useActuatorInfoStore } from '../store/actuatorInfo'
import { useUserStore } from '../store/user'
import { useOidcStore } from '../store/oidc'
import { useNetworksPageStore } from '../store/networksPage'
import { useRoute, useRouter } from 'vue-router'

const uiConfigurationStore = useUiConfiguration()
const actuatorInfoStore = useActuatorInfoStore()
const userStore = useUserStore()
const oidcStore = useOidcStore()
const networksPageStore = useNetworksPageStore()
const dropdownVisible = ref(false)
const menuNavbarRef = ref(null)
const togglerRef = ref(null)
const router = useRouter()
const route = useRoute()
const roles = ref([])
const { faqPage: featureFlagsFAQ } = useFeatureFlags()
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
const isAdminActive = computed(() => {
  return route.path === '/admin' || route.params.userRole === 'ROLE_ADMIN'
})
const isResearcherActive = computed(() => {
  return route.path === '/researcher' || route.params.userRole === 'ROLE_RESEARCHER'
})
const isRepresentativeActive = computed(() => {
  return route.path === '/biobanker' || route.params.userRole === 'ROLE_REPRESENTATIVE'
})
const isGovernanceActive = computed(() => {
  return (
    route.path === '/governance' ||
    route.params.userRole === 'ROLE_REPRESENTATIVE' ||
    route.params.userRole === 'ROLE_ADMIN'
  )
})
const isNetworksActive = computed(() => {
  return route.path.startsWith('/networks')
})
const isFaqActive = computed(() => {
  return route.path === '/FAQ'
})
const isGuideActive = computed(() => {
  return route.path === '/guide'
})
const navLinkStyle = (isActive) => ({
  color: isActive
    ? uiConfiguration.value?.navbarActiveTextColor
    : uiConfiguration.value?.navbarTextColor,
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

function handleOutsideClick(event) {
  const menuEl = menuNavbarRef.value
  if (!menuEl || !menuEl.classList.contains('show')) {
    return
  }
  if (menuEl.contains(event.target) || togglerRef.value?.contains(event.target)) {
    return
  }
  Collapse.getInstance(menuEl)?.hide()
}

onMounted(() => {
  document.addEventListener('click', handleOutsideClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutsideClick)
})
</script>

<style>
nav {
  width: 100%;
  font-size: 0.8125rem;
  text-align: left;
}

.nav-link.nav-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.nav-link.nav-option.active {
  font-weight: 600;
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

.navbar-nav {
  margin-left: 3px;
}

@media (max-width: 991.98px) {
  .navbar-toggler-mobile-left {
    order: -3;
  }

  .navbar-toggler {
    padding-left: 0 !important;
  }

  .navbar-logo {
    order: -2;
    margin-right: auto;
  }

  .navbar-notifications,
  .navbar-profile,
  .navbar-env-indicator {
    order: -1;
  }
}
</style>
