<template>
  <div class="admin-settings-page container-fluid pb-4">
    <h1 class="mb-4 text-center v-step-settings-0">Administrator Console</h1>
    <hr class="mb-4" />

    <div class="row g-0 main-content">
      <div class="col-auto">
        <div
          class="nav flex-row flex-xl-column nav-pills pb-3 pb-xl-0 me-3 pe-3 h-100"
          id="v-pills-tab"
          role="tablist"
        >
          <router-link
            v-for="(item, index) in navItems"
            :key="item.pathPrefix"
            :to="item.to"
            class="nav-link text-lg-start mb-1 rounded"
            :class="[
              route.path.startsWith(item.pathPrefix)
                ? 'active bg-primary text-white'
                : 'text-secondary',
              `v-step-settings-${index + 1}`,
            ]"
            :title="item.description"
          >
            {{ item.label }}
          </router-link>
        </div>
      </div>
      <div class="col">
        <div class="ps-3">
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../store/user.js'
import { useVueTourStore } from '../store/vueTour'

const route = useRoute()
const userStore = useUserStore()
const vueTourStore = useVueTourStore()

const navItems = [
  {
    label: 'UI Configuration',
    to: { name: 'admin-ui-configuration' },
    pathPrefix: '/settings/ui-configuration',
    description: 'Customize the user interface settings.',
  },
  {
    label: 'Users',
    to: { name: 'admin-users' },
    pathPrefix: '/settings/users',
    description: 'Manage user accounts and permissions.',
  },
  {
    label: 'Email Notifications',
    to: { name: 'admin-email-notifications' },
    pathPrefix: '/settings/email-notifications',
    description: 'View and manage email notification settings.',
  },
  {
    label: 'Email Templates',
    to: { name: 'admin-email-templates' },
    pathPrefix: '/settings/email-templates',
    description: 'Create and edit email templates used for notifications.',
  },
  {
    label: 'Access Forms',
    to: { name: 'admin-access-forms' },
    pathPrefix: '/settings/access-forms',
    description: 'Configure access request forms and settings.',
  },
  {
    label: 'Access Form Elements',
    to: { name: 'admin-form-elements' },
    pathPrefix: '/settings/form-elements',
    description: 'Manage elements within the application.',
  },
  {
    label: 'Information Requirements',
    to: { name: 'admin-information-requirements' },
    pathPrefix: '/settings/information-requirements',
    description: 'Configure the information requirements for resources.',
  },
  {
    label: 'Webhooks',
    to: { name: 'admin-webhooks' },
    pathPrefix: '/settings/webhooks',
    description: 'Manage webhook subscriptions and deliveries.',
  },
]

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
  vueTourStore.isSettingsVisible = true
})
</script>

<style scoped>
@media (min-width: 1200px) {
  .nav {
    border-right: 1px solid rgba(0, 0, 0, 0.125);
  }
}
.main-content {
  min-height: calc(100vh - (234px + 56px + 4.5rem + 110px));
}
a:link {
  text-decoration: none;
}
.nav-link:hover:not(.active) {
  background-color: #f8f9fa;
  color: #212529;
}
</style>
