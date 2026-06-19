<template>
  <div class="webhook-detail-page">
    <LoadingIndicator v-if="isLoading" />

    <div v-else-if="webhook" class="specific-area panel panel-default border-">
      <AdminSettingsPageHeader :title="`Edit webhook ${webhook.id}`" />
      <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item" role="presentation">
          <button
            class="nav-link"
            :class="{ active: activeTab === 'configuration' }"
            type="button"
            role="tab"
            @click="activeTab = 'configuration'"
          >
            Configuration
          </button>
        </li>
        <li class="nav-item" role="presentation">
          <button
            class="nav-link"
            :class="{ active: activeTab === 'deliveries' }"
            type="button"
            role="tab"
            @click="activeTab = 'deliveries'"
          >
            Delivery History
          </button>
        </li>
      </ul>

      <div class="tab-content mt-3">
        <div v-show="activeTab === 'configuration'">
          <WebhookConfig
            :form="form"
            :urlIsValid="urlIsValid"
            :showConfiguredSecretBanner="showConfiguredSecretBanner"
            :isConfiguredWebhookWithSecret="isConfiguredWebhookWithSecret"
            :changeSecretMode="changeSecretMode"
            :showSecretValidationError="showSecretValidationError"
            :secretValidationMessage="secretValidationMessage"
            secretInputId="editWebhookSecretInput"
            v-model:secretInput="secretInput"
            @updateForm="updateForm"
            @startSecretChange="startSecretChange"
            @cancelSecretChange="cancelSecretChange"
          />
        </div>

        <div v-show="activeTab === 'deliveries'">
          <DeliveryHistory :deliveries="webhook.deliveries" @redeliver="handleWebhookRedelivery" />
        </div>
      </div>

      <div v-if="activeTab === 'configuration'" class="mt-4 d-flex gap-2">
        <button
          type="button"
          class="btn btn-primary"
          @click="submitForm"
          :disabled="!urlIsValid || !secretIsValid || isSaving"
        >
          Update
        </button>
        <button
          type="button"
          class="btn btn-outline-secondary"
          :disabled="isSaving"
          @click="cancel"
        >
          Cancel
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin.js'
import { useNotificationsStore } from '@/store/notifications.js'
import DeliveryHistory from '@/components/DeliveryHistory.vue'
import LoadingIndicator from '@/components/LoadingIndicator.vue'
import WebhookConfig from '@/components/WebhookConfig.vue'
import AdminSettingsPageHeader from '@/components/AdminSettingsPageHeader.vue'
import {
  buildWebhookPayload,
  setSecretChangeMode,
  useWebhookFormValidation,
} from '@/composables/useWebhookFormValidation.js'

const props = defineProps({
  webhookId: {
    type: [String, Number],
    required: true,
  },
})

const router = useRouter()
const adminStore = useAdminStore()
const notifications = useNotificationsStore()

const webhook = ref(null)
const isLoading = ref(true)
const isSaving = ref(false)
const activeTab = ref('configuration')
const secretInput = ref('')
const changeSecretMode = ref(false)

const form = reactive({
  url: '',
  sslVerification: true,
  active: true,
})

const hasConfiguredSecret = computed(() => Boolean(webhook.value?.secretId))
const isConfiguredWebhookWithSecret = computed(() => hasConfiguredSecret.value)
const showConfiguredSecretBanner = computed(
  () => isConfiguredWebhookWithSecret.value && !changeSecretMode.value,
)
const showSecretInput = computed(() => !hasConfiguredSecret.value || changeSecretMode.value)

const { urlIsValid, secretValidationMessage, showSecretValidationError, secretIsValid } =
  useWebhookFormValidation({
    url: computed(() => form.url),
    secretInput,
    showSecretInput,
  })

const isValidWebhookId = (value) => {
  const id = Number(value)
  return Number.isInteger(id) && id > 0
}

const resetFormFromWebhook = (currentWebhook) => {
  form.url = currentWebhook.url
  form.sslVerification = currentWebhook.sslVerification
  form.active = currentWebhook.active
  setSecretChangeMode(changeSecretMode, secretInput, false)
}

const retrieveWebhook = async () => {
  if (!isValidWebhookId(props.webhookId)) {
    notifications.setNotification('Webhook not found', 'danger')
    await router.replace({ name: 'admin-webhooks' })
    return
  }

  try {
    isLoading.value = true
    const currentWebhook = await adminStore.getWebhook(props.webhookId)

    if (!currentWebhook?.id) {
      throw new Error('Webhook not found')
    }

    webhook.value = currentWebhook
    resetFormFromWebhook(currentWebhook)
  } catch {
    notifications.setNotification('Webhook not found', 'danger')
    await router.replace({ name: 'admin-webhooks' })
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  await retrieveWebhook()
})

watch(
  () => props.webhookId,
  async () => {
    await retrieveWebhook()
  },
)

const updateForm = (updatedForm) => {
  Object.assign(form, updatedForm)
}

const startSecretChange = () => {
  setSecretChangeMode(changeSecretMode, secretInput, true)
}

const cancelSecretChange = () => {
  setSecretChangeMode(changeSecretMode, secretInput, false)
}

const cancel = () => {
  router.push({ name: 'admin-webhooks' })
}

const buildUpdateWebhookPayload = () => {
  return buildWebhookPayload({
    form,
    secretInput: secretInput.value,
    secretIsValid: secretIsValid.value,
    clearExistingSecret: isConfiguredWebhookWithSecret.value && changeSecretMode.value,
  })
}

const submitForm = async () => {
  if (!urlIsValid.value || !secretIsValid.value || !webhook.value?.id) {
    return
  }

  try {
    isSaving.value = true
    const updatedWebhook = await adminStore.updateWebhook(
      webhook.value.id,
      buildUpdateWebhookPayload(),
    )
    if (!updatedWebhook) {
      notifications.setNotification('Webhook update failed', 'danger')
      return
    }
    await router.push({ name: 'admin-webhooks' })
  } catch (error) {
    console.error('Error updating webhook:', error)
    notifications.setNotification('Webhook update failed', 'danger')
  } finally {
    isSaving.value = false
  }
}

const handleWebhookRedelivery = async (deliveryId) => {
  if (!webhook.value?.id) {
    return
  }

  try {
    isSaving.value = true
    await adminStore.redeliverWebhookDelivery(webhook.value.id, deliveryId)
    webhook.value = await adminStore.getWebhook(webhook.value.id)
    notifications.setNotification('Webhook redelivery completed', 'success')
  } catch (error) {
    console.error('Error redelivering webhook delivery:', error)
    notifications.setNotification('Webhook redelivery failed', 'danger')
  } finally {
    isSaving.value = false
  }
}
</script>
