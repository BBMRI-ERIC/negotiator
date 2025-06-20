<template>
  <div v-if="deliveries && deliveries.length">
    <ul class="list-group">
      <li v-for="delivery in deliveries" :key="delivery.id" class="list-group-item">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <strong>Status:</strong>
            <span v-if="delivery.httpStatusCode === 200" class="text-success"> 200 OK </span>
            <span v-else class="text-danger"> {{ delivery.httpStatusCode }} Error </span>
          </div>
          <small>{{ formatDate(delivery.at) }}</small>
        </div>

        <!-- Collapsible JSON payload section -->
        <div class="mt-2">
          <button class="btn btn-sm btn-outline-secondary" @click="togglePayload(delivery.id)">
            {{ expandedPayloads[delivery.id] ? 'Hide' : 'Show' }} Payload
          </button>

          <div v-if="expandedPayloads[delivery.id]" class="mt-2">
            <pre class="p-2 bg-light rounded"><code>{{ formatJson(delivery.content) }}</code></pre>
          </div>
          <div v-else class="mt-1">
            <small class="text-muted">
              {{ previewJson(delivery.content) }}
            </small>
          </div>
        </div>

        <div v-if="delivery.errorMessage" class="mt-1">
          <small class="text-muted">
            {{
              delivery.errorMessage.length > 100
                ? delivery.errorMessage.substring(0, 100) + '...'
                : delivery.errorMessage
            }}
          </small>
        </div>
      </li>
    </ul>
  </div>
  <div v-else>
    <p>No delivery history available.</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  deliveries: {
    type: Array,
    required: true,
  },
})

const expandedPayloads = ref({})

const togglePayload = (id) => {
  expandedPayloads.value[id] = !expandedPayloads.value[id]
}

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

const previewJson = (json) => {
  try {
    const str = typeof json === 'string' ? json : JSON.stringify(json)
    return str.length > 100 ? str.substring(0, 100) + '...' : str
  } catch {
    return 'Invalid JSON data'
  }
}

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleString()
}
</script>
