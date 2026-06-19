<template>
  <div class="webhook-create-page">
    <div class="specific-area panel panel-default border-">
      <AdminSettingsPageHeader title="Add Webhook" />
      <WebhookConfig
        :form="form"
        :urlIsValid="urlIsValid"
        :showSecretValidationError="showSecretValidationError"
        :secretValidationMessage="secretValidationMessage"
        secretInputId="newWebhookSecretInput"
        v-model:secretInput="secretInput"
        @updateForm="updateForm"
      />

      <div class="d-flex gap-2 mt-4">
        <button
          type="button"
          class="btn btn-primary"
          @click="submitForm"
          :disabled="!urlIsValid || !secretIsValid || isSaving"
        >
          Create
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
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin.js'
import { useNotificationsStore } from '@/store/notifications.js'
import WebhookConfig from '@/components/WebhookConfig.vue'
import AdminSettingsPageHeader from '@/components/AdminSettingsPageHeader.vue'
import {
  buildWebhookPayload,
  useWebhookFormValidation,
} from '@/composables/useWebhookFormValidation.js'

const router = useRouter()
const adminStore = useAdminStore()
const notifications = useNotificationsStore()

const isSaving = ref(false)
const secretInput = ref('')

const form = reactive({
  url: '',
  sslVerification: true,
  active: true,
})

const { urlIsValid, secretValidationMessage, showSecretValidationError, secretIsValid } =
  useWebhookFormValidation({
    url: computed(() => form.url),
    secretInput,
  })

const updateForm = (updatedForm) => {
  Object.assign(form, updatedForm)
}

const cancel = () => {
  router.push({ name: 'admin-webhooks' })
}

const submitForm = async () => {
  if (!urlIsValid.value || !secretIsValid.value) {
    return
  }

  try {
    isSaving.value = true
    const createdWebhook = await adminStore.createWebhook(
      buildWebhookPayload({
        form,
        secretInput: secretInput.value,
        secretIsValid: secretIsValid.value,
      }),
    )
    if (!createdWebhook) {
      notifications.setNotification('Error creating webhook', 'danger')
      return
    }
    await router.push({ name: 'admin-webhooks' })
  } catch (error) {
    console.error('Error creating webhook:', error)
    notifications.setNotification('Error creating webhook', 'danger')
  } finally {
    isSaving.value = false
  }
}
</script>
