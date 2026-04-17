<template>
  <div class="admin-settings-page">
    <div v-if="!isLoading">
      <h1 class="mb-5 text-center v-step-settings-0">Administrator Console</h1>
      <hr />
      <InformationRequirementsSection
        class="v-step-settings-1"
        :resource-all-events="resourceAllEvents"
        :info-requirements="infoRequirements"
        :access-forms="accessForms"
        @set-info-requirements="setInfoRequirements"
        @add-requirement="() => {}"
      />
      <hr />
      <WebhooksSection class="v-step-settings-2" />
      <hr />
      <EmailNotificationsSection class="v-step-settings-3" @view-email="viewEmailDetails" />
      <hr />
      <UserListSection class="v-step-settings-4" />
    </div>
    <LoadingIndicator v-else />
    <EmailDetailModal id="emailDetailModal" :email-id="selectedEmailId" />
    <hr />
    <email-template-section class="v-step-settings-5" />
    <hr />
    <div class="v-step-settings-6">
      <access-forms-section />
    </div>
    <ElementsManagement class="v-step-settings-7" />
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
a:link {
  text-decoration: none;
}
</style>
