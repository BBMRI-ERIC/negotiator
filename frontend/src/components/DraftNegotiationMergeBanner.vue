<template>
  <div v-if="draftNegotiation" class="alert alert-info mt-3 mb-3" role="alert">
    <div class="d-flex flex-column flex-md-row align-items-start align-items-md-center">
      <div class="flex-grow-1 mb-2 mb-md-0">
        <i class="bi bi-info-circle me-2"></i>
        <strong>Draft found:</strong>
        Would you like to merge the selected resources into the request you started
        <strong>{{ formatTimeAgo(draftNegotiation.creationDate) }}</strong>?
      </div>
      <div class="d-flex gap-2 ms-md-3">
        <button @click="handleMerge" class="btn btn-sm btn-primary">
          <i class="bi bi-arrow-left-right me-1"></i>
          Merge
        </button>
        <button @click="handleDismiss" class="btn btn-sm btn-outline-secondary">
          Dismiss
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  draftNegotiation: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['merge', 'dismiss'])

function handleMerge() {
  emit('merge', props.draftNegotiation)
}

function handleDismiss() {
  emit('dismiss')
}

function formatTimeAgo(dateString) {
  if (!dateString) return 'recently'

  const date = new Date(dateString)
  const now = new Date()
  const diffInMs = now - date
  const diffInMinutes = Math.floor(diffInMs / (1000 * 60))
  const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60))
  const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24))

  if (diffInMinutes < 1) {
    return 'just now'
  } else if (diffInMinutes < 60) {
    return `${diffInMinutes} minute${diffInMinutes > 1 ? 's' : ''} ago`
  } else if (diffInHours < 24) {
    return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`
  } else if (diffInDays < 7) {
    return `${diffInDays} day${diffInDays > 1 ? 's' : ''} ago`
  } else if (diffInDays < 30) {
    const weeks = Math.floor(diffInDays / 7)
    return `${weeks} week${weeks > 1 ? 's' : ''} ago`
  } else if (diffInDays < 365) {
    const months = Math.floor(diffInDays / 30)
    return `${months} month${months > 1 ? 's' : ''} ago`
  } else {
    const years = Math.floor(diffInDays / 365)
    return `${years} year${years > 1 ? 's' : ''} ago`
  }
}
</script>

<style scoped>
.alert {
  border-radius: 0.375rem;
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
}

.btn-sm {
  white-space: nowrap;
  font-size: 0.875rem;
  padding: 0.375rem 0.75rem;
}

.gap-2 {
  gap: 0.5rem;
}

@media (max-width: 767.98px) {
  .d-flex.flex-column {
    width: 100%;
  }

  .d-flex.gap-2 {
    width: 100%;
  }

  .btn-sm {
    flex: 1;
  }
}
</style>
