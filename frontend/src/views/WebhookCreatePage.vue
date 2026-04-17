<template>
  <div class="webhook-create-page">
    <AdminBreadcrumb :segments="breadcrumbSegments" />

    <div class="specific-area panel panel-default border-">
      <WebhookConfig
        :form="form"
        :urlIsValid="urlIsValid"
        :showConfiguredSecretBanner="showConfiguredSecretBanner"
        :isConfiguredWebhookWithSecret="isConfiguredWebhookWithSecret"
        :changeSecretMode="changeSecretMode"
        :showSecretValidationError="showSecretValidationError"
        :secretValidationMessage="secretValidationMessage"
        secretInputId="newWebhookSecretInput"
        v-model:secretInput="secretInput"
        @updateForm="updateForm"
        @startSecretChange="startSecretChange"
        @cancelSecretChange="cancelSecretChange"
      />

      <div class="d-flex justify-content-center mt-4">
        <button
          type="button"
          class="btn btn-primary me-2"
          @click="submitForm"
          :disabled="!urlIsValid || !secretIsValid || isSaving"
        >
          Create
        </button>
        <button
          type="button"
          class="btn btn-outline-secondary"
          @click="goBack"
          :disabled="isSaving"
        >
          Cancel
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin.js'
import { useNotificationsStore } from '@/store/notifications.js'
import AdminBreadcrumb from '@/components/AdminBreadcrumb.vue'
import WebhookConfig from '@/components/WebhookConfig.vue'

const router = useRouter()
const adminStore = useAdminStore()
const notifications = useNotificationsStore()

const isSaving = ref(false)
const secretInput = ref('')
const changeSecretMode = ref(false)

const form = reactive({
  url: '',
  sslVerification: true,
  active: true,
})

const breadcrumbSegments = [
  { label: 'Admin Settings', to: '/settings' },
  { label: 'Webhooks', to: '/settings/webhooks' },
  { label: 'Add Webhook' },
]

const showConfiguredSecretBanner = computed(() => false)
const isConfiguredWebhookWithSecret = computed(() => false)
const showSecretInput = computed(() => true)

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
  }

  return payload
}

const submitForm = async () => {
  if (!urlIsValid.value || !secretIsValid.value) {
    return
  }

  try {
    isSaving.value = true
    await adminStore.createWebhook(buildWebhookPayload())
    await router.push('/settings/webhooks')
  } catch (error) {
    console.error('Error creating webhook:', error)
    notifications.setNotification('Error creating webhook', 'danger')
  } finally {
    isSaving.value = false
  }
}

const goBack = async () => {
  await router.push('/settings/webhooks')
}
</script>
