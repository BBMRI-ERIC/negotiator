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
                <WebhookConfig :form="form" :urlIsValid="urlIsValid" />
              </div>
              <div
                class="tab-pane fade"
                id="deliveries"
                role="tabpanel"
                aria-labelledby="deliveries-tab"
              >
                <DeliveryHistory :deliveries="webhook.deliveries" />
              </div>
            </div>
          </div>
          <!-- For new webhooks, just show the configuration form -->
          <div v-else>
            <WebhookConfig
              :form="form"
              :urlIsValid="urlIsValid"
              @update:form="updateForm"
            />

          </div>
        </div>
        <div class="modal-footer justify-content-center">
          <button
            type="button"
            class="btn btn-primary me-2"
            @click="submitForm"
            :disabled="!urlIsValid"
          >
            {{ isNew ? 'Create' : 'Update' }}
          </button>
          <button type="button" class="btn btn-close-custom" data-bs-dismiss="modal">
            Close
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import WebhookConfig from '../WebhookConfig.vue'
import DeliveryHistory from '../DeliveryHistory.vue'

const props = defineProps({
  shown: { type: Boolean, required: true },
  webhook: { type: Object, required: true },
  id: { type: String, default: 'editWebhookModal' },
  fade: { type: Boolean, default: true }
})
const emit = defineEmits(['update', 'create'])

// Initialize a reactive form object
const form = reactive({
  url: props.webhook.url,
  sslVerification: props.webhook.sslVerification,
  active: props.webhook.active
})

// Determine if this is a new webhook (i.e. no ID exists)
const isNew = computed(() => !props.webhook.id)

// Update the form when the webhook prop changes
watch(() => props.webhook, newWebhook => {
  form.url = newWebhook.url
  form.sslVerification = newWebhook.sslVerification
  form.active = newWebhook.active
})

// URL validation logic
const urlIsValid = computed(() => {
  const pattern = /^https?:\/\/.+/
  return pattern.test(form.url)
})

const updateForm = (updatedForm) => {
  Object.assign(form, updatedForm)
}


const submitForm = () => {
  if (urlIsValid.value) {
    if (isNew.value) {
      emit('create', form)
    } else {
      emit('update', form)
    }
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
