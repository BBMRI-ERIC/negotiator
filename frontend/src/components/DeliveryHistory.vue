<template>
  <div v-if="deliveries && deliveries.length">
    <div class="accordion delivery-history-accordion">
      <div v-for="delivery in deliveries" :key="delivery.id" class="accordion-item">
        <div class="accordion-header">
          <button
            class="accordion-button collapsed delivery-header d-flex align-items-center flex-wrap gap-2"
            type="button"
            data-bs-toggle="collapse"
            :data-bs-target="`#delivery-history-collapse-${delivery.id}`"
            aria-expanded="false"
            :aria-controls="`delivery-history-collapse-${delivery.id}`"
          >
            <DeliveryStatus :delivery="delivery" />
            <DeliveryId :id="delivery.rootId" />
            <DeliveryEvent :event="delivery.content.type" />
            <small class="ms-auto text-nowrap">{{ formatDate(delivery.at) }}</small>
          </button>
        </div>

        <div :id="`delivery-history-collapse-${delivery.id}`" class="accordion-collapse collapse">
          <div class="accordion-body pt-2">
            <ul class="nav nav-tabs mb-3">
              <li class="nav-item">
                <span class="nav-link active">Request</span>
              </li>
              <li v-if="delivery.httpStatusCode" class="nav-item d-flex align-items-center">
                <span class="nav-link disabled">
                  Response
                  <DeliveryResponsCode :delivery="delivery" />
                </span>
              </li>
              <li v-else class="nav-item d-flex align-items-center">
                <span class="nav-link disabled">No Response</span>
              </li>
              <div class="d-flex ms-auto align-self-center">
                <button
                  class="btn btn-sm btn-outline-secondary ms-auto"
                  @click="$emit('redeliver', delivery.id)"
                >
                  Redeliver
                </button>
              </div>
            </ul>

            <div v-if="delivery.errorMessage" class="alert alert-warning">
              <div>
                <i class="bi bi-exclamation-triangle" aria-hidden="true" />
                <span> This payload could not be delivered </span>
              </div>
              <code class="text-danger">
                {{
                  delivery.errorMessage.length > 100
                    ? delivery.errorMessage.substring(0, 100) + '...'
                    : delivery.errorMessage
                }}
              </code>
            </div>

            <div>
              <pre
                class="p-2 mb-0 bg-light rounded"
              ><code>{{ formatJson(delivery.content) }}</code></pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else>
    <p>No delivery history available.</p>
  </div>
</template>

<style scoped>
/*
  Override Bootstrap's auto margin on thearrow used to align the
  arrow to the right, we set the auto margin on the timestamp to
  begin alignment to the right from there
*/
.accordion-button::after {
  margin-left: 0;
}
</style>

<script setup>
import DeliveryEvent from './DeliveryEvent.vue'
import DeliveryId from './DeliveryId.vue'
import DeliveryStatus from './DeliveryStatus.vue'
import DeliveryResponsCode from './DeliveryResponsCode.vue'

defineProps({
  deliveries: {
    type: Array,
    required: true,
  },
})

defineEmits(['redeliver'])

const formatJson = (json) => {
  try {
    if (typeof json === 'string') {
      return JSON.stringify(JSON.parse(json), null, 2)
    }
    return JSON.stringify(json, null, 2)
  } catch {
    return json
  }
}

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleString()
}
</script>
