<template>
  <div class="fixed-top  mt-5 pt-3" >
    <div class="col-12" v-for="notification in currentNotifications">
      <div class="alert alert-dismissible fade show" :class="returnColor(notification.type)" role="alert">
        <span class="alert-icons me-3">
          <i v-if="notification.type === 'info'" class="bi bi-info-circle" />
          <i v-if="notification.type === 'success'" class="bi bi-check-circle" />
          <i v-if="notification.type === 'warning' || notification.type === 'danger'" class="bi bi-exclamation-triangle" />
        </span>
        {{ notification.message }}
        <button
          type="button"
          class="btn-close"
          data-bs-dismiss="alert"
          aria-label="Close"
          @click="resetNotification"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useNotificationsStore } from '../store/notifications'

const notificationsStore = useNotificationsStore()

const currentNotifications = computed(() => {
  return notificationsStore.currentNotifications
})

function returnColor(type) {
  if (type) return 'alert-' + type

  return 'alert-warning'
}

function resetNotification() {
  notificationsStore.resetNotification
}
</script>
