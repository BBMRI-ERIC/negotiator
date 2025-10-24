<template>
  <div :style="{ 'background-color': uiConfiguration?.appBackgroundColor }">
    <VueTour v-if="isVueTourVisible" />
    <AnalyticsNotice :privacy-link="privacyPolicyLink" />

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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useNotificationsStore } from '@/store/notifications.js'
import allFeatureFlags from '@/config/featureFlags.js'
import VueTour from './components/VueTour.vue'
import AllVueTours from './components/vue-tours/AllVueTours.vue'
import NavigationBar from './components/NavigationBar.vue'
import AlertNotification from './components/AlertNotification.vue'
import AnalyticsNotice from './components/AnalyticsNotice.vue'
import Footer from './components/FooterComp.vue'
import errorPage from '@/views/ErrorPage.vue'
import { useUiConfiguration } from '@/store/uiConfiguration.js'

const uiConfigurationStore = useUiConfiguration()
const useNotifications = useNotificationsStore()
const route = useRoute()
const router = useRouter()

const vueTourFeatureFlag = !!(
  allFeatureFlags.vueTour === 'true' || allFeatureFlags.vueTour === true
)

onMounted(async () => {
  await updateFaviconUrl()
  changeFaviconUrl()
})

watch(
  () => router.currentRoute.value.fullPath,
  (newVal, oldVal) => {
    if (oldVal !== '/') {
      useNotifications.criticalError = false
    }
  },
)

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

const privacyPolicyLink = computed(() => {
  return uiConfigurationStore.uiConfiguration?.footer?.footerPrivacyPolicyLink
})
const faviconUrl = ref('../public/favicon.ico')

async function updateFaviconUrl() {
  const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  try {
    await uiConfigurationStore.retrieveUiConfiguration()
    if (uiConfiguration.value?.faviconUrlDark && uiConfiguration.value?.faviconUrlLight) {
      faviconUrl.value = isDark
        ? uiConfiguration.value.faviconUrlDark
        : uiConfiguration.value.faviconUrlLight
    } else {
      faviconUrl.value = '../public/favicon.ico'
    }
  } catch {
    faviconUrl.value = '../public/favicon.ico'
  }
}

function changeFaviconUrl() {
  const link = document.querySelector("link[rel~='icon']")
  if (!link) {
    const newLink = document.createElement('link')
    newLink.rel = 'icon'
    newLink.href = faviconUrl.value
    document.head.appendChild(newLink)
  } else {
    link.href = faviconUrl.value
  }
}
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
