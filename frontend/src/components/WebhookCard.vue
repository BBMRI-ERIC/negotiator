<template>
  <div class="card mb-3">
    <div class="card-body d-flex justify-content-between align-items-center">
      <div class="d-flex align-items-center">
        <i :class="getStatusIcon(webhook)" class="me-3"></i>
        <div>
          <button class="url-link card-title mb-0" @click.stop="$emit('edit', webhook)">
            {{ webhook.url }}
          </button>
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
  if (webhook.testInProgress) {
    return 'bi bi-arrow-repeat spin'
  }
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
</script>

<style scoped>
.btn-info {
  color: white;
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.url-link {
  background: none;
  border: 0;
  color: var(--bs-link-color);
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}

.url-link:hover {
  color: var(--bs-link-hover-color);
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
