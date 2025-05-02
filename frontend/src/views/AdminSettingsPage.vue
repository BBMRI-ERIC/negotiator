<template>
  <div v-if="!isLoading">
    <h1 class="mb-5 text-center">Administrator Console</h1>
    <hr />
    <InformationRequirementsSection
      :resource-all-events="resourceAllEvents"
      :info-requirements="infoRequirements"
      :access-forms="accessForms"
      @set-info-requirements="setInfoRequirements"
    />
    <hr />
    <WebhooksSection
      :webhooks="webhooks"
      @add-webhook="addWebhook"
      @edit-webhook="editWebhook"
      @delete-webhook="deleteWebhook"
      @test-webhook="testWebhook"
    />
    <hr />
    <UserListSection :users="users" />
  </div>
  <LoadingIndicator v-else />
  <WebhookModal
    id="webhookmodal"
    :shown="shown"
    :webhook="selectedWebhook"
    @update="handleWebhookUpdate"
    @create="handleNewWebhook"
  />
  <confirmation-modal
    id="delete-webhookmodal"
    title="Delete Webhook"
    text="Are you sure you want to delete this webhook?"
    :message-enabled="false"
    @confirm="confirmDeleteWebhook"
    ref="deleteModal"
  />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useUserStore } from '../store/user.js'
import { useAdminStore } from '../store/admin.js'
import { useFormsStore } from '../store/forms.js'
import InformationRequirementsSection from '@/components/InformationRequirementsSection.vue'
import WebhooksSection from '@/components/WebhooksSection.vue'
import UserListSection from '@/components/UserListSection.vue'
import LoadingIndicator from '@/components/LoadingIndicator.vue'
import { Modal } from 'bootstrap'
import { useNotificationsStore } from '@/store/notifications.js'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'
import WebhookModal from '@/components/modals/WebhookModal.vue'

const userStore = useUserStore()
const adminStore = useAdminStore()
const formsStore = useFormsStore()
const notifications = useNotificationsStore()

const resourceAllEvents = ref({})
const infoRequirements = ref([])
const accessForms = ref([])
const users = ref([])
const isLoading = ref(true)
const editModal = ref(undefined)
const selectedWebhook = ref({})
const webhooks = ref([])
const shown = ref(false)

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }

  try {
    isLoading.value = true
    resourceAllEvents.value = await adminStore.retrieveResourceAllEvents()
    infoRequirements.value = await adminStore.retrieveInfoRequirements()
    accessForms.value = await formsStore.retrieveAllAccessForms()
    users.value = await adminStore.retrieveUsers() || []
    const freshWebhooks = await adminStore.retrieveWebhooks()
    webhooks.value = freshWebhooks || []
  } catch (error) {
    console.error('Initialization error:', error)
  } finally {
    isLoading.value = false
  }
})

async function setInfoRequirements(data) {
  await adminStore.setInfoRequirements(data)
  infoRequirements.value = await adminStore.retrieveInfoRequirements()
}

const addWebhook = () => {
  selectedWebhook.value = {
    id: null,
    url: '',
    sslVerification: true,
    active: true,
    deliveries: []
  }
  editModal.value = new Modal(document.querySelector('#webhookmodal'))
  shown.value = true
  editModal.value.show()
}

const editWebhook = (webhook) => {
  selectedWebhook.value = webhook
  editModal.value = new Modal(document.querySelector('#webhookmodal'))
  shown.value = true
  editModal.value.show()
}

const deleteModal = ref(null)
const webhookToDelete = ref(null)

const deleteWebhook = (webhook) => {
  webhookToDelete.value = webhook
  deleteModal.value = new Modal(document.querySelector('#delete-webhookmodal'))
  deleteModal.value.show()
}

const confirmDeleteWebhook = async () => {
  if (!webhookToDelete.value) return

  try {
    isLoading.value = true
    await adminStore.deleteWebhook(webhookToDelete.value.id)
    webhooks.value = await adminStore.retrieveWebhooks() || []
    notifications.setNotification('Webhook deleted successfully')
  } catch (error) {
    console.error('Error deleting webhook:', error)
    notifications.setNotification('Error deleting webhook', 'error')
  } finally {
    isLoading.value = false
    webhookToDelete.value = null
  }
}

const handleWebhookUpdate = async (updatedConfig) => {
  try {
    isLoading.value = true
    await adminStore.updateWebhook(selectedWebhook.value.id, updatedConfig)
    const freshWebhooks = await adminStore.retrieveWebhooks()
    webhooks.value = freshWebhooks || []
  } catch (error) {
    console.error('Error updating webhook:', error)
  } finally {
    isLoading.value = false
    editModal.value.hide()
  }
}

const handleNewWebhook = async (updatedConfig) => {
  try {
    isLoading.value = true
    await adminStore.createWebhook(updatedConfig)
    const freshWebhooks = await adminStore.retrieveWebhooks()
    webhooks.value = freshWebhooks || []
  } catch (error) {
    console.error('Error creating webhook:', error)
    notifications.setNotification('Error creating webhook')
  } finally {
    isLoading.value = false
    editModal.value.hide()
  }
}

const testWebhook = async (webhook) => {
  const index = webhooks.value.findIndex(w => w.id === webhook.id)
  if (index === -1) return

  webhooks.value[index] = { ...webhooks.value[index], testInProgress: true }

  try {
    await adminStore.testWebhook(webhook.id)
    const updatedWebhook = await adminStore.getWebhook(webhook.id)
    webhooks.value[index] = {
      ...updatedWebhook,
      testInProgress: false
    }
  } catch {
    webhooks.value[index] = {
      ...webhooks.value[index],
      testInProgress: false
    }
  }
}
</script>

<style scoped>
a:link {
  text-decoration: none;
}
</style>