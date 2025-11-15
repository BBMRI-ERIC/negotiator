<template>
  <div :style="{ 'background-color': uiConfiguration?.appBackgroundColor }">
    <AllVueTours />
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
import { computed, watch } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { useNotificationsStore } from '@/store/notifications.js'
import AllVueTours from './components/vue-tours/AllVueTours.vue'
import NavigationBar from './components/NavigationBar.vue'
import AlertNotification from './components/AlertNotification.vue'
import Footer from './components/FooterComp.vue'
import errorPage from '@/views/ErrorPage.vue'
import { useUiConfiguration } from '@/store/uiConfiguration.js'

const uiConfigurationStore = useUiConfiguration()
const useNotifications = useNotificationsStore()
const router = useRouter()

watch(
  () => router.currentRoute.value.fullPath,
  (newVal, oldVal) => {
    if (oldVal !== '/') {
      useNotifications.criticalError = false
    }
  },
)

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
