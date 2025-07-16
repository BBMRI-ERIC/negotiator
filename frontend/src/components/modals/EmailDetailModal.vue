<template>
  <div
    class="modal fade"
    :id="id"
    tabindex="-1"
    aria-labelledby="emailDetailModalLabel"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-lg">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="emailDetailModalLabel">Email Notification Details</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>
        <div class="modal-body">
          <div v-if="email">
            <!-- Email metadata -->
            <div class="row mb-4">
              <div class="col-md-6">
                <div class="card">
                  <div class="card-body">
                    <h6 class="card-title">Email Information</h6>
                    <div class="mb-2">
                      <strong>To:</strong>
                      <span class="ms-2">{{ email.address }}</span>
                    </div>
                    <div class="mb-2">
                      <strong>Sent At:</strong>
                      <span class="ms-2">{{ formatDate(email.sentAt) }}</span>
                    </div>
                    <div>
                      <strong>Email ID:</strong>
                      <span class="ms-2 font-monospace text-muted">{{ email.id }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Email content tabs -->
            <ul class="nav nav-tabs" id="emailContentTabs" role="tablist">
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link active"
                  id="rendered-tab"
                  data-bs-toggle="tab"
                  data-bs-target="#rendered"
                  type="button"
                  role="tab"
                  aria-controls="rendered"
                  aria-selected="true"
                >
                  <i class="fas fa-eye me-2"></i>Rendered View
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link"
                  id="source-tab"
                  data-bs-toggle="tab"
                  data-bs-target="#source"
                  type="button"
                  role="tab"
                  aria-controls="source"
                  aria-selected="false"
                >
                  <i class="fas fa-code me-2"></i>HTML Source
                </button>
              </li>
            </ul>

            <div class="tab-content border border-top-0 p-3" id="emailContentTabsContent">
              <!-- Rendered HTML view -->
              <div
                class="tab-pane fade show active"
                id="rendered"
                role="tabpanel"
                aria-labelledby="rendered-tab"
              >
                <div class="email-content-container">
                  <div class="email-content" v-html="sanitizedContent"></div>
                </div>
              </div>

              <!-- HTML source view -->
              <div class="tab-pane fade" id="source" role="tabpanel" aria-labelledby="source-tab">
                <div class="position-relative">
                  <button
                    class="btn btn-sm btn-outline-secondary position-absolute top-0 end-0 mt-2 me-2"
                    @click="copyToClipboard"
                  >
                    <i class="fas fa-copy me-1"></i>Copy
                  </button>
                  <pre class="bg-light p-3 rounded"><code>{{ email.message }}</code></pre>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="text-center py-4">
            <div class="spinner-border" role="status">
              <span class="visually-hidden">Loading email details...</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useEmailStore } from '@/store/emails.js'
import DOMPurify from 'dompurify'

const props = defineProps({
  id: {
    type: String,
    default: 'emailDetailModal',
  },
  emailId: {
    type: [Number, String],
    default: null,
  },
})

const emailStore = useEmailStore()
const email = ref(null)
const loading = ref(false)

const sanitizedContent = computed(() => {
  if (!email.value?.message) return ''

  // Sanitize HTML content to prevent XSS attacks
  return DOMPurify.sanitize(email.value.message, {
    // Allow safe HTML tags for email content
    ALLOWED_TAGS: [
      'p', 'br', 'div', 'span', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'strong', 'b', 'em', 'i', 'u', 'ul', 'ol', 'li', 'a', 'img',
      'table', 'thead', 'tbody', 'tr', 'td', 'th', 'blockquote',
      'pre', 'code', 'hr', 'small', 'sub', 'sup'
    ],
    // Allow safe attributes
    ALLOWED_ATTR: [
      'href', 'title', 'alt', 'src', 'width', 'height', 'style',
      'class', 'id', 'target', 'rel'
    ],
    // Only allow safe protocols for links and images
    ALLOWED_URI_REGEXP: /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|cid|xmpp|data):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i,
    // Clean up HTML structure
    KEEP_CONTENT: true,
    // Remove scripts and other dangerous content
    FORBID_TAGS: ['script', 'object', 'embed', 'form', 'input', 'button'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover'],
  })
})

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    timeZoneName: 'short',
  })
}

const copyToClipboard = async () => {
  try {
    await navigator.clipboard.writeText(email.value.message)
    // You might want to show a toast notification here
    console.log('Email content copied to clipboard')
  } catch (err) {
    console.error('Failed to copy: ', err)
    // Fallback for older browsers
    const textArea = document.createElement('textarea')
    textArea.value = email.value.message
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
  }
}

const fetchEmailDetails = async (id) => {
  if (!id) return

  loading.value = true
  try {
    email.value = await emailStore.fetchNotificationEmailById(id)
  } catch (error) {
    console.error('Error fetching email details:', error)
    email.value = null
  } finally {
    loading.value = false
  }
}

// Watch for emailId changes to fetch new email details
watch(
  () => props.emailId,
  (newId) => {
    if (newId) {
      fetchEmailDetails(newId)
    } else {
      email.value = null
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.email-content-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  padding: 1rem;
  background-color: #fff;
}

.email-content {
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.6;
  color: #333;
}

/* Style rendered email content */
.email-content :deep(h1),
.email-content :deep(h2),
.email-content :deep(h3),
.email-content :deep(h4),
.email-content :deep(h5),
.email-content :deep(h6) {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
  color: #2c3e50;
}

.email-content :deep(p) {
  margin-bottom: 1rem;
}

.email-content :deep(a) {
  color: #007bff;
  text-decoration: none;
}

.email-content :deep(a:hover) {
  text-decoration: underline;
}

.email-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 0.25rem;
}

.email-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1rem;
}

.email-content :deep(th),
.email-content :deep(td) {
  padding: 0.5rem;
  border: 1px solid #dee2e6;
  text-align: left;
}

.email-content :deep(th) {
  background-color: #f8f9fa;
  font-weight: 600;
}

.email-content :deep(blockquote) {
  border-left: 4px solid #007bff;
  padding-left: 1rem;
  margin: 1rem 0;
  font-style: italic;
  color: #6c757d;
}

.email-content :deep(pre) {
  background-color: #f8f9fa;
  padding: 1rem;
  border-radius: 0.25rem;
  overflow-x: auto;
}

.email-content :deep(code) {
  background-color: #f8f9fa;
  padding: 0.2rem 0.4rem;
  border-radius: 0.25rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

pre code {
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
