<template>
  <div :style="{ 'background-color': uiConfiguration?.appBackgroundColor }">
    <VueTour v-if="isVueTourVisible" />

    <header>
      <navigation-bar />
    </header>
    <div v-if="$route.path !== '/'" class="mt-5 pt-4">
      <AlertNotification />
      &nbsp;
    </div>
    <div class="container body d-flex flex-column">
      <div class="row">
        <div class="col-12">
          <errorPage v-if="useNotifications.criticalError" />
          <router-view v-else :key="$route.path" />
        </div>
      </div>
    </div>
    <div v-if="$route.path !== '/'" class="container">
      <div class="col-12">
        <Footer />
      </div>
    </div>

    <!-- Release Notification Modal at App level -->
    <ReleaseNotificationModal
      modal-id="globalReleaseModal"
      :release="releasesStore.latestRelease"
      @dismiss="handleReleaseDissmiss"
    />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { useNotificationsStore } from '@/store/notifications.js'
import { useReleasesStore } from '@/store/releases.js'
import { useActuatorInfoStore } from '@/store/actuatorInfo.js'
import allFeatureFlags from '@/config/featureFlags.js'
import VueTour from './components/VueTour.vue'
import NavigationBar from './components/NavigationBar.vue'
import AlertNotification from './components/AlertNotification.vue'
import Footer from './components/FooterComp.vue'
import errorPage from '@/views/ErrorPage.vue'
import ReleaseNotificationModal from './components/modals/ReleaseNotificationModal.vue'
import { useUiConfiguration } from '@/store/uiConfiguration.js'

const uiConfigurationStore = useUiConfiguration()
const useNotifications = useNotificationsStore()
const releasesStore = useReleasesStore()
const actuatorInfoStore = useActuatorInfoStore()
const route = useRoute()

const vueTourFeatureFlag = !!(
  allFeatureFlags.vueTour === 'true' || allFeatureFlags.vueTour === true
)

// Initialize release checking system at app level
onMounted(() => {
  releasesStore.loadDismissedReleases()

  // Set current version when actuator info is available
  if (actuatorInfoStore.actuatorInfoBuildVersion) {
    releasesStore.setCurrentVersion(actuatorInfoStore.actuatorInfoBuildVersion)
    // Only start periodic checks if version pattern is supported
    if (isVersionSupported(actuatorInfoStore.actuatorInfoBuildVersion)) {
      releasesStore.startPeriodicCheck()
    }
  } else {
    // Wait for actuator info to be loaded
    actuatorInfoStore.retrieveBackendActuatorInfo().then(() => {
      releasesStore.setCurrentVersion(actuatorInfoStore.actuatorInfoBuildVersion)
      // Only start periodic checks if version pattern is supported
      if (isVersionSupported(actuatorInfoStore.actuatorInfoBuildVersion)) {
        releasesStore.startPeriodicCheck()
      }
    })
  }
})

function isVersionSupported(version) {
  // Check if version follows v3.x.x pattern (e.g., v3.17.3, v3.0.0, v3.1.2)
  const versionPattern = /^v3\.\d+\.\d+$/
  return versionPattern.test(version)
}

function handleReleaseDissmiss(tagName) {
  releasesStore.dismissRelease(tagName)
}

const isVueTourVisible = computed(() => {
  return (
    (route.fullPath === '/researcher' ||
      route.fullPath === '/admin' ||
      route.fullPath === '/biobanker') &&
    vueTourFeatureFlag
  )
})

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})
</script>

<style scoped>
.box {
  inline-size: 300px;
}
header {
  line-height: 1.5;
  max-height: 100vh;
}

.body {
  min-height: calc(100vh - 391px);
}

@media (min-width: 1024px) {
  header {
    display: flex;
    place-items: center;
    padding-right: calc(var(--section-gap) / 2);
  }

  .body {
    min-height: calc(100vh - 263px);
  }

  header .wrapper {
    display: flex;
    place-items: flex-start;
    flex-wrap: wrap;
  }
}
</style>
