<template>
  <Transition name="slide-up">
    <div
      v-if="!noticeDismissed"
      class="analytics-notice"
    >
      <div class="analytics-notice-container">
        <div class="analytics-notice-content">
          <i class="bi bi-info-circle"></i>
          <span>
            We use cookies and browser storage to analyse traffic on our websites and to maintain your login session. All personal data is anonymized and not shared with third parties!
            <a
              v-if="privacyLink"
              href="#"
              class="analytics-link"
              @click.prevent="openPrivacyPolicy"
            >
              Click here for more information.
            </a>
          </span>
        </div>
        <button
          class="btn-close-notice"
          @click="dismissNotice"
          aria-label="Close notice"
        >
          <i class="bi bi-x-lg"></i>
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  privacyLink: {
    type: String,
    default: null,
  },
})

const NOTICE_KEY = 'negotiator_analytics_notice_dismissed'

// Check localStorage immediately to prevent flash of banner
const dismissed = localStorage.getItem(NOTICE_KEY)
const noticeDismissed = ref(dismissed === 'true')

const openPrivacyPolicy = () => {
  if (props.privacyLink) {
    window.open(props.privacyLink, '_blank')
  }
}

const dismissNotice = () => {
  noticeDismissed.value = true
  localStorage.setItem(NOTICE_KEY, 'true')
}
</script>

<style scoped>
.analytics-notice {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: rgba(255, 255, 255, 0.98);
  color: #333;
  padding: 16px 20px;
  z-index: 9999;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  max-width: 500px;
  border-left: 4px solid #667eea;
}

.analytics-notice-container {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.analytics-notice-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
  font-size: 0.9rem;
  line-height: 1.5;
}

.analytics-notice-content i.bi-info-circle {
  font-size: 1.2rem;
  color: #667eea;
  flex-shrink: 0;
  margin-top: 2px;
}

.analytics-link {
  color: #667eea;
  text-decoration: underline;
  font-weight: 500;
  white-space: nowrap;
}

.analytics-link:hover {
  color: #5568d3;
}

.btn-close-notice {
  background: transparent;
  border: none;
  color: #666;
  cursor: pointer;
  padding: 0;
  font-size: 1rem;
  line-height: 1;
  flex-shrink: 0;
  transition: color 0.2s;
}

.btn-close-notice:hover {
  color: #333;
}

/* Slide up animation */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from {
  transform: translateY(100px);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateY(100px);
  opacity: 0;
}

/* Responsive design */
@media (max-width: 768px) {
  .analytics-notice {
    bottom: 10px;
    right: 10px;
    left: 10px;
    max-width: none;
  }

  .analytics-notice-content {
    font-size: 0.85rem;
  }
}
</style>

