<template>
  <div
    class="modal"
    :class="{ fade: fade }"
    tabindex="-1"
    :aria-labelledby="`${id}Label`"
    aria-hidden="true"
    :id="id"
  >
    <div class="modal-dialog modal-dialog-centered modal-xl">
      <div class="modal-content">
        <div class="modal-header justify-content-center">
          <h4 class="modal-title text-center">
            {{ isNew ? 'Add Webhook' : 'Edit Webhook ' + webhook.id }}
          </h4>
        </div>
        <div class="modal-body">
          <!-- For existing webhooks, show tabs for configuration and delivery history -->
          <div v-if="!isNew">
            <ul class="nav nav-tabs" id="editWebhookTabs" role="tablist">
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link active"
                  id="config-tab"
                  data-bs-toggle="tab"
                  data-bs-target="#config"
                  type="button"
                  role="tab"
                  aria-controls="config"
                  aria-selected="true"
                >
                  Configuration
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link"
                  id="deliveries-tab"
                  data-bs-toggle="tab"
                  data-bs-target="#deliveries"
                  type="button"
                  role="tab"
                  aria-controls="deliveries"
                  aria-selected="false"
                >
                  Delivery History
                </button>
              </li>
            </ul>
            <div class="tab-content mt-3" id="editWebhookTabsContent">
              <div
                class="tab-pane fade show active"
                id="config"
                role="tabpanel"
                aria-labelledby="config-tab"
              >
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
              <div
                class="tab-pane fade"
                id="deliveries"
                role="tabpanel"
                aria-labelledby="deliveries-tab"
              >
                <DeliveryHistory
                  :deliveries="webhook.deliveries"
                  @redeliver="$emit('redeliver', { webhookId: webhook.id, deliveryId: $event })"
                />
              </div>
            </div>
          </div>
          <!-- For new webhooks, just show the configuration form -->
          <div v-else>
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
          </div>
        </div>
        <div class="modal-footer justify-content-center">
          <button
            type="button"
            class="btn btn-primary me-2"
            @click="submitForm"
            :disabled="!urlIsValid || !secretIsValid"
          >
            {{ isNew ? 'Create' : 'Update' }}
          </button>
          <button type="button" class="btn btn-close-custom" data-bs-dismiss="modal">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import WebhookConfig from '../WebhookConfig.vue'
import DeliveryHistory from '../DeliveryHistory.vue'

const props = defineProps({
  shown: { type: Boolean, required: true },
  webhook: { type: Object, required: true },
  id: { type: String, default: 'editWebhookModal' },
  fade: { type: Boolean, default: true },
})
const emit = defineEmits(['update', 'create', 'redeliver'])
const secretInput = ref('')
const changeSecretMode = ref(false)

// Initialize a reactive form object using the webhook prop.
const form = reactive({
  url: props.webhook.url,
  sslVerification: props.webhook.sslVerification,
  active: props.webhook.active,
})

// Determine if this is a new webhook (i.e. no ID exists)
const isNew = computed(() => !props.webhook.id)
const hasConfiguredSecret = computed(() => Boolean(props.webhook.secretId))
const isConfiguredWebhookWithSecret = computed(() => !isNew.value && hasConfiguredSecret.value)
const showConfiguredSecretBanner = computed(
  () => isConfiguredWebhookWithSecret.value && !changeSecretMode.value,
)
const showSecretInput = computed(
  () => isNew.value || !hasConfiguredSecret.value || changeSecretMode.value,
)

// Update the form when the webhook prop changes.
watch(
  () => props.webhook,
  (newWebhook) => {
    form.url = newWebhook.url
    form.sslVerification = newWebhook.sslVerification
    form.active = newWebhook.active
    secretInput.value = ''
    changeSecretMode.value = false
  },
  { deep: true },
)

// URL validation logic.
const urlIsValid = computed(() => {
  const pattern = /^https?:\/\/.+/
  return pattern.test(form.url)
})

const secretIsValid = computed(() => {
  return secretValidation.value.isValid
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

const showSecretValidationError = computed(() => {
  return showSecretInput.value && secretValidationMessage.value.length > 0
})

const decodeBase64KeyMaterial = (encodedKeyMaterial) => {

  if (!/^[A-Za-z0-9+/=]+$/.test(encodedKeyMaterial)) {
    throw new Error('Invalid base64 characters')
  }

  const decodedBinary = atob(encodedKeyMaterial)
  return Uint8Array.from(decodedBinary, (char) => char.charCodeAt(0))
}

// This handler is triggered when the child emits the updateForm event.
const updateForm = (updatedForm) => {
  Object.assign(form, updatedForm)
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

const startSecretChange = () => {
  changeSecretMode.value = true
  secretInput.value = ''
}

const cancelSecretChange = () => {
  changeSecretMode.value = false
  secretInput.value = ''
}

// Submit the form by emitting create/update events.
const submitForm = () => {
  if (!urlIsValid.value || !secretIsValid.value) {
    return
  }

  const payload = buildWebhookPayload()
  if (isNew.value) {
    emit('create', payload)
  } else {
    emit('update', payload)
  }
}
</script>

<style scoped>
.modal-body {
  max-height: 70vh;
  overflow-y: auto;
}

.btn-close-custom {
  background: gray;
  border-color: gray;
  outline-color: gray;
  color: white;
}

.btn-close-custom:hover {
  background: darkgray;
}

.modal-header,
.modal-footer {
  justify-content: center;
}
</style>
