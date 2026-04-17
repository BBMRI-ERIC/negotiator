<template>
  <div class="webhook-detail-page">
    <AdminBreadcrumb :segments="breadcrumbSegments" />

    <LoadingIndicator v-if="isLoading" />

    <div v-else-if="webhook" class="specific-area panel panel-default border-">
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

      <div class="mt-4">
        <button
          v-if="activeTab === 'configuration'"
          type="button"
          class="btn btn-primary"
          @click="submitForm"
          :disabled="!urlIsValid || !secretIsValid || isSaving"
        >
          Update
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
import AdminBreadcrumb from '@/components/AdminBreadcrumb.vue'
import DeliveryHistory from '@/components/DeliveryHistory.vue'
import LoadingIndicator from '@/components/LoadingIndicator.vue'
import WebhookConfig from '@/components/WebhookConfig.vue'

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

const breadcrumbSegments = computed(() => [
  { label: 'Admin Settings', to: '/settings' },
  { label: 'Webhooks', to: '/settings/webhooks' },
  { label: `Webhook ${webhook.value?.id ?? props.webhookId}` },
])

const hasConfiguredSecret = computed(() => Boolean(webhook.value?.secretId))
const isConfiguredWebhookWithSecret = computed(() => hasConfiguredSecret.value)
const showConfiguredSecretBanner = computed(
  () => isConfiguredWebhookWithSecret.value && !changeSecretMode.value,
)
const showSecretInput = computed(() => !hasConfiguredSecret.value || changeSecretMode.value)

const urlIsValid = computed(() => {
  const pattern = /^https?:\/\/.+/
  return pattern.test(form.url)
})

const secretValidation = computed(() => {
  if (!showSecretInput.value) {
    return { isValid: true, message: '' }
  }

  if (secretInput.value.length === 0) {
    return { isValid: true, message: '' }
  }

  if (!secretInput.value.startsWith('whsec_')) {
    return { isValid: false, message: 'Secret must start with whsec_' }
  }

  const encodedKeyMaterial = secretInput.value.slice('whsec_'.length)
  if (encodedKeyMaterial.length === 0) {
    return {
      isValid: false,
      message: 'Secret must include base64 key material after whsec_',
    }
  }

  let decodedKeyMaterial
  try {
    decodedKeyMaterial = decodeBase64KeyMaterial(encodedKeyMaterial)
  } catch {
    return {
      isValid: false,
      message: 'Secret key material must be valid base64',
    }
  }

  if (decodedKeyMaterial.length < 24 || decodedKeyMaterial.length > 64) {
    return {
      isValid: false,
      message: 'Secret key material must decode to between 24 and 64 bytes',
    }
  }

  return { isValid: true, message: '' }
})

const secretValidationMessage = computed(() => secretValidation.value.message)
const showSecretValidationError = computed(
  () => showSecretInput.value && secretValidationMessage.value.length > 0,
)
const secretIsValid = computed(() => secretValidation.value.isValid)

const decodeBase64KeyMaterial = (encodedKeyMaterial) => {
  if (!/^[A-Za-z0-9+/=]+$/.test(encodedKeyMaterial)) {
    throw new Error('Invalid base64 characters')
  }

  const decodedBinary = atob(encodedKeyMaterial)
  return Uint8Array.from(decodedBinary, (char) => char.charCodeAt(0))
}

const isValidWebhookId = (value) => {
  const id = Number(value)
  return Number.isInteger(id) && id > 0
}

const resetFormFromWebhook = (currentWebhook) => {
  form.url = currentWebhook.url
  form.sslVerification = currentWebhook.sslVerification
  form.active = currentWebhook.active
  secretInput.value = ''
  changeSecretMode.value = false
}

const retrieveWebhook = async () => {
  if (!isValidWebhookId(props.webhookId)) {
    notifications.setNotification('Webhook not found', 'danger')
    await router.replace('/settings/webhooks')
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
    await router.replace('/settings/webhooks')
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
  changeSecretMode.value = true
  secretInput.value = ''
}

const cancelSecretChange = () => {
  changeSecretMode.value = false
  secretInput.value = ''
}

const buildWebhookPayload = () => {
  const payload = {
    url: form.url,
    sslVerification: form.sslVerification,
    active: form.active,
  }

  if (secretInput.value.length > 0 && secretIsValid.value) {
    payload.secret = secretInput.value
  } else if (isConfiguredWebhookWithSecret.value && changeSecretMode.value) {
    payload.secret = null
  }

  return payload
}

const submitForm = async () => {
  if (!urlIsValid.value || !secretIsValid.value || !webhook.value?.id) {
    return
  }

  try {
    isSaving.value = true
    await adminStore.updateWebhook(webhook.value.id, buildWebhookPayload())
    await router.push('/settings/webhooks')
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
