<template>
  <div class="admin-settings-page container-fluid pb-4">
    <h1 class="mb-4 text-center v-step-settings-0">Administrator Console</h1>
    <hr class="mb-4" />

    <div class="row g-0 main-content">
      <div class="col-auto">
        <div
          class="nav flex-row flex-md-column nav-pills pb-3 pb-md-0 me-3 pe-3 h-100"
          id="v-pills-tab"
          role="tablist"
        >
          <button
            v-for="(item, index) in navItems"
            :key="item.id"
            class="nav-link text-md-start mb-1 rounded"
            :class="{
              'active bg-primary text-white': activeNavItemIndex === index,
              'text-secondary': activeNavItemIndex !== index,
              'v-step-settings-1': index === 0,
              'v-step-settings-2': index === 1,
              'v-step-settings-3': index === 2,
              'v-step-settings-4': index === 3,
              'v-step-settings-5': index === 4,
              'v-step-settings-6': index === 5,
              'v-step-settings-7': index === 6,
              'v-step-settings-8': index === 7,
            }"
            data-bs-toggle="pill"
            @click="activeNavItemIndex = index"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
      <div class="col">
        <div class="tab-content ps-3" id="v-pills-tabContent">
          <div class="tab-pane fade show active">
            <div v-if="!isLoading">
              <InformationRequirementsSection
                v-if="activeNavItemIndex === 0"
                :resource-all-events="resourceAllEvents"
                :info-requirements="infoRequirements"
                :access-forms="accessForms"
                @set-info-requirements="setInfoRequirements"
                @add-requirement="() => {}"
              />
              <WebhooksSection v-if="activeNavItemIndex === 1" />
              <EmailNotificationsSection
                v-if="activeNavItemIndex === 2"
                @view-email="viewEmailDetails"
              />
              <UserListSection v-if="activeNavItemIndex === 3" />
              <email-template-section v-if="activeNavItemIndex === 4" />
              <access-forms-section v-if="activeNavItemIndex === 5" />
              <ElementsManagement v-if="activeNavItemIndex === 6" />
              <AdminSettingsUiConfiguration v-if="activeNavItemIndex === 7" />
            </div>
            <div v-else>
              <LoadingIndicator />
            </div>
          </div>
          <EmailDetailModal :email-id="selectedEmailId" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useUserStore } from '../store/user.js'
import { useAdminStore } from '../store/admin.js'
import { useFormsStore } from '../store/forms.js'
import { useVueTourStore } from '../store/vueTour'
import InformationRequirementsSection from '@/components/InformationRequirementsSection.vue'
import WebhooksSection from '@/components/WebhooksSection.vue'
import EmailNotificationsSection from '@/components/EmailNotificationsSection.vue'
import UserListSection from '@/components/UserListSection.vue'
import LoadingIndicator from '@/components/LoadingIndicator.vue'
import { Modal } from 'bootstrap'
import EmailDetailModal from '@/components/modals/EmailDetailModal.vue'
import EmailTemplateSection from '@/components/TemplateSection.vue'
import AccessFormsSection from '@/components/AccessFormsSection.vue'
import AdminSettingsUiConfiguration from '@/components/AdminSettingsUiConfiguration.vue'

import ElementsManagement from '@/components/ElementsManagement.vue'
const userStore = useUserStore()
const adminStore = useAdminStore()
const formsStore = useFormsStore()
const vueTourStore = useVueTourStore()

const resourceAllEvents = ref({})
const infoRequirements = ref([])
const accessForms = ref([])
const isLoading = ref(true)
const selectedEmailId = ref(null)

const activeNavItemIndex = ref(0)
const navItems = [
  {
    id: 1,
    label: 'Information Requirements',
    description: 'Configure the information requirements for resources.',
  },
  {
    id: 2,
    label: 'Webhooks',
    description: 'Manage webhook subscriptions and deliveries.',
  },
  {
    id: 3,
    label: 'Email Notifications',
    description: 'View and manage email notification settings.',
  },
  {
    id: 4,
    label: 'User Management',
    description: 'Manage user accounts and permissions.',
  },
  {
    id: 5,
    label: 'Email Templates',
    description: 'Create and edit email templates used for notifications.',
  },
  {
    id: 6,
    label: 'Access Forms',
    description: 'Configure access request forms and settings.',
  },
  {
    id: 7,
    label: 'Form Elements Management',
    description: 'Manage elements within the application.',
  },
  {
    id: 8,
    label: 'UI Configuration',
    description: 'Customize the user interface settings.',
  },
]

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }

  try {
    isLoading.value = true
    resourceAllEvents.value = await adminStore.retrieveResourceAllEvents()
    infoRequirements.value = await adminStore.retrieveInfoRequirements()
    accessForms.value = await formsStore.retrieveAllAccessForms()
  } catch (error) {
    console.error('Initialization error:', error)
  } finally {
    isLoading.value = false
    vueTourStore.isSettingsVisible = true
  }
})

async function setInfoRequirements(data) {
  await adminStore.setInfoRequirements(data)
  infoRequirements.value = await adminStore.retrieveInfoRequirements()
}

const viewEmailDetails = (email) => {
  selectedEmailId.value = email.id
  const emailModal = new Modal(document.querySelector('#emailDetailModal'))
  emailModal.show()
}
</script>

<style scoped>
@media (min-width: 768px) {
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
</style>
