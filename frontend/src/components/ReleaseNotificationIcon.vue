<template>
  <div v-if="isAdmin && hasNewRelease" class="release-notification">
    <button
      type="button"
      class="btn btn-link p-0 release-trigger"
      @click="showModal"
      :title="`New release available: ${latestRelease?.tag_name}`"
    >
      <i class="bi bi-triangle-fill text-danger flashing-icon"></i>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useReleasesStore } from '../store/releases.js'
import Modal from 'bootstrap/js/dist/modal'
import { ROLES } from '@/config/consts.js'

const releasesStore = useReleasesStore()

const hasNewRelease = computed(() => releasesStore.hasNewRelease)
const latestRelease = computed(() => releasesStore.latestRelease)

function showModal() {
  const modalElement = document.getElementById('globalReleaseModal')
  if (modalElement) {
    const modal = new Modal(modalElement)
    modal.show()
  }
}
const isAdmin = computed(() => {
  return roles.value.includes(ROLES.ADMINISTRATOR)
})
</script>

<style scoped>
.release-notification {
  position: relative;
  display: inline-block;
}

.release-trigger {
  border: none !important;
  background: none !important;
  padding: 0 !important;
  font-size: 1.2rem;
}

.release-trigger:hover {
  transform: scale(1.1);
  transition: transform 0.2s ease-in-out;
}

.flashing-icon {
  animation: flash 1.5s infinite;
  filter: drop-shadow(0 0 2px rgba(220, 53, 69, 0.3));
}

@keyframes flash {
  0%, 50% {
    opacity: 1;
  }
  25%, 75% {
    opacity: 0.3;
  }
}

.release-trigger:focus {
  outline: 2px solid #dc3545;
  outline-offset: 2px;
  border-radius: 2px;
}
</style>
