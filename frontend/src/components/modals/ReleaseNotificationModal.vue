<template>
  <NegotiatorModal :id="modalId" title="New Release Available" :is-modal-small="false">
    <template #body>
      <div v-if="release" class="release-info text-start">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <div>
            <span class="badge bg-success me-2">{{ release.tag_name }}</span>
            <small class="text-muted">
              Released {{ formatDate(release.published_at) }}
            </small>
          </div>
          <a
            :href="release.html_url"
            target="_blank"
            class="btn btn-outline-primary btn-sm"
          >
            <i class="bi bi-box-arrow-up-right me-1"></i>
            View on GitHub
          </a>
        </div>

        <div class="changelog">
          <h6><i class="bi bi-github me-2"></i>{{ release?.name || release?.tag_name }}</h6>
          <div
            class="release-notes"
            v-html="formattedReleaseBody"
          ></div>
        </div>
      </div>
      <div v-else class="text-center">
        <div class="spinner-border" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </div>
    </template>
  </NegotiatorModal>
</template>

<script setup>
import { computed, defineProps } from 'vue'
import { marked } from 'marked'
import NegotiatorModal from './NegotiatorModal.vue'

const props = defineProps({
  release: {
    type: Object,
    default: null
  },
  modalId: {
    type: String,
    default: 'releaseModal'
  }
})

const formattedReleaseBody = computed(() => {
  if (!props.release?.body) return ''

  // Convert markdown to HTML
  try {
    return marked.parse(props.release.body)
  } catch {
    // Fallback to plain text with line breaks
    return props.release.body.replace(/\n/g, '<br>')
  }
})

function formatDate(dateString) {
  if (!dateString) return ''

  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}
</script>

<style scoped>
.release-notes {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e9ecef;
  border-radius: 0.375rem;
  padding: 1rem;
  background-color: #f8f9fa;
}

.release-notes :deep(h1),
.release-notes :deep(h2),
.release-notes :deep(h3) {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
}

.release-notes :deep(h1) {
  font-size: 1.2rem;
}

.release-notes :deep(h2) {
  font-size: 1.1rem;
}

.release-notes :deep(h3) {
  font-size: 1rem;
}

.release-notes :deep(ul),
.release-notes :deep(ol) {
  padding-left: 1.5rem;
}

.release-notes :deep(code) {
  background-color: #e9ecef;
  padding: 0.2rem 0.4rem;
  border-radius: 0.2rem;
  font-size: 0.875rem;
}

.release-notes :deep(pre) {
  background-color: #e9ecef;
  padding: 0.75rem;
  border-radius: 0.375rem;
  overflow-x: auto;
}

.release-notes :deep(blockquote) {
  border-left: 4px solid #dee2e6;
  padding-left: 1rem;
  margin-left: 0;
  color: #6c757d;
}
</style>
