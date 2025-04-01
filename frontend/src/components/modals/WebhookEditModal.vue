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
                <form @submit.prevent="submitForm">
                  <div class="mb-3">
                    <label for="webhookUrl" class="form-label">Webhook URL</label>
                    <input
                      type="url"
                      class="form-control"
                      id="webhookUrl"
                      v-model="form.url"
                      required
                      :class="{ 'is-invalid': !urlIsValid }"
                      title="Must be a valid URL starting with http:// or https://"
                    />
                    <small class="form-text text-muted">
                      Enter the full URL where you want to receive webhook notifications.
                    </small>
                    <div v-if="!urlIsValid" class="invalid-feedback">
                      Please enter a valid URL starting with http:// or https://
                    </div>
                  </div>
                  <div class="mb-3">
                    <label class="form-label">SSL Verification</label>
                    <div>
                      <div class="form-check form-check-inline">
                        <input
                          class="form-check-input"
                          type="radio"
                          name="sslVerification"
                          id="sslEnabled"
                          :value="true"
                          v-model="form.sslVerification"
                        />
                        <label class="form-check-label" for="sslEnabled">
                          Enabled
                        </label>
                      </div>
                      <div class="form-check form-check-inline">
                        <input
                          class="form-check-input"
                          type="radio"
                          name="sslVerification"
                          id="sslDisabled"
                          :value="false"
                          v-model="form.sslVerification"
                        />
                        <label class="form-check-label" for="sslDisabled">
                          Disabled
                        </label>
                      </div>
                    </div>
                    <small class="form-text text-warning" v-if="form.sslVerification === false">
                      Disabling SSL verification is not recommended.
                    </small>
                  </div>
                  <div class="mb-3 form-check">
                    <input
                      type="checkbox"
                      class="form-check-input"
                      id="active"
                      v-model="form.active"
                    />
                    <label class="form-check-label" for="active">
                      Active - Toggle to enable or disable webhook notifications.
                    </label>
                  </div>
                </form>
              </div>
              <div
                class="tab-pane fade"
                id="deliveries"
                role="tabpanel"
                aria-labelledby="deliveries-tab"
              >
                <div v-if="webhook.deliveries && webhook.deliveries.length">
                  <ul class="list-group">
                    <li
                      v-for="delivery in webhook.deliveries"
                      :key="delivery.id"
                      class="list-group-item"
                    >
                      <div class="d-flex justify-content-between align-items-center">
                        <div>
                          <strong>Status:</strong>
                          <span v-if="delivery.httpStatusCode === 200" class="text-success">
                            200 OK
                          </span>
                          <span v-else class="text-danger">
                            {{ delivery.httpStatusCode }} Error
                          </span>
                        </div>
                        <small>{{ delivery.at }}</small>
                      </div>
                      <div class="mt-1">
                        <small class="text-muted">
                          {{ delivery.content }}
                        </small>
                      </div>
                      <div v-if="delivery.errorMessage" class="mt-1">
                        <small class="text-muted">
                          {{ delivery.errorMessage.length > 100 ? delivery.errorMessage.substring(0, 100) + '...' : delivery.errorMessage
                          }}
                        </small>
                      </div>
                    </li>
                  </ul>
                </div>
                <div v-else>
                  <p>No delivery history available.</p>
                </div>
              </div>
            </div>
          </div>
          <div v-else>
            <form @submit.prevent="submitForm">
              <div class="mb-3">
                <label for="webhookUrl" class="form-label">Webhook URL</label>
                <input
                  type="url"
                  class="form-control"
                  id="webhookUrl"
                  v-model="form.url"
                  required
                  :class="{ 'is-invalid': !urlIsValid }"
                  title="Must be a valid URL starting with http:// or https://"
                />
                <small class="form-text text-muted">
                  Enter the full URL where you want to receive webhook notifications.
                </small>
                <div v-if="!urlIsValid" class="invalid-feedback">
                  Please enter a valid URL starting with http:// or https://
                </div>
              </div>
              <div class="mb-3">
                <label class="form-label">SSL Verification</label>
                <div>
                  <div class="form-check form-check-inline">
                    <input
                      class="form-check-input"
                      type="radio"
                      name="sslVerification"
                      id="sslEnabled"
                      :value="true"
                      v-model="form.sslVerification"
                    />
                    <label class="form-check-label" for="sslEnabled">
                      Enabled
                    </label>
                  </div>
                  <div class="form-check form-check-inline">
                    <input
                      class="form-check-input"
                      type="radio"
                      name="sslVerification"
                      id="sslDisabled"
                      :value="false"
                      v-model="form.sslVerification"
                    />
                    <label class="form-check-label" for="sslDisabled">
                      Disabled
                    </label>
                  </div>
                </div>
                <small class="form-text text-warning" v-if="form.sslVerification === false">
                  Disabling SSL verification is not recommended.
                </small>
              </div>
              <div class="mb-3 form-check">
                <input
                  type="checkbox"
                  class="form-check-input"
                  id="active"
                  v-model="form.active"
                />
                <label class="form-check-label" for="active">
                  Active - Toggle to enable or disable webhook notifications.
                </label>
              </div>
            </form>
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

const props = defineProps({
  shown: { type: Boolean, required: true },
  webhook: { type: Object, required: true },
  id: { type: String, default: 'editWebhookModal' },
  fade: { type: Boolean, default: true }
})
const emit = defineEmits(['update', 'create'])
const form = reactive({
  url: props.webhook.url,
  sslVerification: props.webhook.sslVerification,
  active: props.webhook.active
})
const isNew = computed(() => !props.webhook.id)
watch(() => props.webhook, newWebhook => {
  form.url = newWebhook.url
  form.sslVerification = newWebhook.sslVerification
  form.active = newWebhook.active
})
const urlIsValid = computed(() => {
  const pattern = /^https?:\/\/.+/
  return pattern.test(form.url)
})
const submitForm = () => {
  if (urlIsValid.value) {
    if (isNew.value) {
      emit('create', { ...form })
    } else {
      emit('update', { ...form })
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
