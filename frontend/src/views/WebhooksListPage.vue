<template>
  <div class="webhooks-page">
    <AdminBreadcrumb :segments="breadcrumbSegments" />

    <div v-if="!isLoading" class="specific-area panel panel-default border-">
      <div class="d-flex justify-content-between align-items-center mb-1">
        <h1 class="text-left h2 mb-0">Webhooks</h1>
        <button class="btn btn-success" @click="addWebhook">Add Webhook</button>
      </div>
      <div class="text-muted mb-3">
        Webhooks allow external services to be notified when certain events happen. When the
        specified events occur, we send a POST request to each URL provided. Learn more in our
        <a
          href="https://bbmri-eric.github.io/negotiator/administrator#webhooks"
          target="_blank"
          rel="noopener noreferrer"
          >Webhooks Documentation</a
        >.
      </div>

      <div v-if="webhooks.length === 0" class="alert alert-light my-3">No webhooks configured.</div>

      <WebhookCard
        v-else
        v-for="webhook in webhooks"
        :key="webhook.id"
        :webhook="webhook"
        @edit="editWebhook"
        @delete="deleteWebhook"
        @test="testWebhook"
      />
    </div>

    <LoadingIndicator v-else />

    <ConfirmationModal
      id="delete-webhookmodal"
      title="Delete Webhook"
      text="Are you sure you want to delete this webhook?"
      :message-enabled="false"
      @confirm="confirmDeleteWebhook"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Modal } from 'bootstrap'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin.js'
import { useNotificationsStore } from '@/store/notifications.js'
import AdminBreadcrumb from '@/components/AdminBreadcrumb.vue'
import LoadingIndicator from '@/components/LoadingIndicator.vue'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'
import WebhookCard from '@/components/WebhookCard.vue'

const router = useRouter()
const adminStore = useAdminStore()
const notifications = useNotificationsStore()

const isLoading = ref(true)
const webhooks = ref([])
const deleteModal = ref(null)
const webhookToDelete = ref(null)

const breadcrumbSegments = [{ label: 'Admin Settings', to: '/settings' }, { label: 'Webhooks' }]

const retrieveWebhooks = async () => {
  webhooks.value = (await adminStore.retrieveWebhooks()) || []
}

onMounted(async () => {
  try {
    isLoading.value = true
    await retrieveWebhooks()
  } finally {
    isLoading.value = false
  }
})

const addWebhook = () => {
  router.push('/settings/webhooks/new')
}

const editWebhook = (webhook) => {
  router.push(`/settings/webhooks/${webhook.id}`)
}

const deleteWebhook = (webhook) => {
  webhookToDelete.value = webhook

  if (!deleteModal.value) {
    deleteModal.value = new Modal(document.querySelector('#delete-webhookmodal'))
  }

  deleteModal.value.show()
}

const confirmDeleteWebhook = async () => {
  if (!webhookToDelete.value) {
    return
  }

  try {
    isLoading.value = true
    await adminStore.deleteWebhook(webhookToDelete.value.id)
    await retrieveWebhooks()
  } catch (error) {
    console.error('Error deleting webhook:', error)
    notifications.setNotification('Error deleting webhook', 'danger')
  } finally {
    isLoading.value = false
    webhookToDelete.value = null
  }
}

const testWebhook = async (webhook) => {
  const index = webhooks.value.findIndex((webhookItem) => webhookItem.id === webhook.id)
  if (index === -1) {
    return
  }

  webhooks.value[index] = { ...webhooks.value[index], testInProgress: true }

  try {
    await adminStore.testWebhook(webhook.id)
    const updatedWebhook = await adminStore.getWebhook(webhook.id)
    webhooks.value[index] = {
      ...updatedWebhook,
      testInProgress: false,
    }
  } catch {
    webhooks.value[index] = {
      ...webhooks.value[index],
      testInProgress: false,
    }
  }
}
</script>
