<template>
  <div
    v-if="notification !== undefined"
    class="fixed-top mt-5 pt-3"
  >
    <div class="col-12">
      <div
        class="alert alert-dismissible fade show"
        :class="returnColor"
        role="alert"
      >
        {{ notification }}
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

const notification = computed(() => {
  return notificationsStore.notification.message
})

const returnColor = computed(() => {
  if (notificationsStore.notification.type)
    return 'alert-' + notificationsStore.notification.type

  return 'alert-warning'
})

function resetNotification() {
  notificationsStore.resetNotification
}
</script>