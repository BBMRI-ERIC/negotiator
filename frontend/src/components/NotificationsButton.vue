<template>
  <div class="dropdown">
    <button
      type="button"
      class="btn btn-sm rounded-circle position-relative py-0 px-1"
      :class="getAllNotifications.length > 0 ? '' : 'disabled'"
      :style="{ color: uiConfiguration?.navbarButtonOutlineColor }"
      data-bs-toggle="dropdown"
      aria-expanded="false"
      @click="resetCurrentNotifications"
    >
      <i class="bi bi-bell" />

      <span
        v-if="getAllNotifications.length > 0"
        class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger pb-0 px-1"
      >
        {{ getAllNotifications.length }}
      </span>
    </button>
    <ul class="dropdown-menu dropdown-menu-end">
      <li class="dropdown-item alert-warning">Notifications:</li>
      <li>
        <hr class="dropdown-divider" />
      </li>
      <li
        v-for="notification in getAllNotifications"
        :key="notification"
        class="dropdown-item py-0"
      >
        <div class="alert mb-1 d-flex py-2 mb-2" :class="returnColor(notification)" role="alert">
          <span class="me-auto">{{ notification.message }}</span>
          <span class="ms-3 text-muted">{{ notification.timestamp }}</span>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import { useNotificationsStore } from '../store/notifications.js'

const uiConfigurationStore = useUiConfiguration()
const notificationsStore = useNotificationsStore()

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.navbar
})
const getAllNotifications = computed(() => {
  return notificationsStore.allNotifications
})

function returnColor(notification) {
  if (notification.type) return 'alert-' + notification.type

  return 'alert-warning'
}

async function resetCurrentNotifications() {
  notificationsStore.resetCurrentNotifications()
}
</script>

<style scoped>
.dropdown-menu {
  overflow: hidden;
  overflow-y: auto;
  max-height: calc(100vh - 150px);
}
</style>
