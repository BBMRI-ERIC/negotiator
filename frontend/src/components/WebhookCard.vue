<template>
  <div class="card mb-3">
    <div class="card-body d-flex justify-content-between align-items-center">
      <div class="d-flex align-items-center">
        <i :class="getStatusIcon(webhook)" class="me-3"></i>
        <div>
          <h5 class="card-title mb-0">{{ webhook.url }}</h5>
          <small class="text-muted">
            <span v-if="webhook.testInProgress">
              <i class="bi bi-arrow-repeat spin"></i> Testing...
            </span>
            <span v-else>
              {{ getLastDeliveryStatus(webhook) }}
            </span>
          </small>
        </div>
      </div>
      <div>
        <button
          class="btn btn-outline-secondary btn-sm me-2"
          @click.stop="$emit('test', webhook)"
          :disabled="webhook.testInProgress"
        >
          Test
        </button>
        <button class="btn btn-primary btn-sm me-2" @click.stop="$emit('edit', webhook)">
          Edit
        </button>
        <button class="btn btn-danger btn-sm" @click.stop="$emit('delete', webhook)">Delete</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  webhook: {
    type: Object,
    required: true,
  },
})

const getStatusIcon = (webhook) => {
  if (!webhook.active) {
    return 'bi bi-dash-circle text-secondary'
  }
  if (webhook.deliveries && webhook.deliveries.length > 0) {
    const latest = webhook.deliveries[0]
    return latest.httpStatusCode === 200
      ? 'bi bi-check-circle text-success'
      : 'bi bi-exclamation-triangle text-danger'
  }
  return 'bi bi-question-circle text-secondary'
}

const getLastDeliveryStatus = (webhook) => {
  if (webhook.deliveries && webhook.deliveries.length > 0) {
    const latest = webhook.deliveries[0]
    return latest.httpStatusCode === 200 ? '200 OK' : `${latest.httpStatusCode} Error`
  }
  return 'No deliveries'
}
</script>

<style scoped>
.btn-info {
  color: white;
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
