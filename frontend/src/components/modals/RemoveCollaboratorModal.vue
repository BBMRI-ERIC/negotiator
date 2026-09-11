<template>
  <div
    v-if="isOpen && collaborator"
    class="modal fade show"
    tabindex="-1"
    role="dialog"
    aria-modal="true"
    aria-labelledby="remove-collaborator-title"
    style="display: block; background-color: rgba(0, 0, 0, 0.5)"
    @click.self="cancel"
  >
    <div class="modal-dialog modal-dialog-centered modal-sm" @click.stop>
      <div class="modal-content">
        <div class="modal-header">
          <h6 id="remove-collaborator-title" class="modal-title">Remove Collaborator</h6>
          <button
            type="button"
            class="btn-close"
            aria-label="Close"
            :disabled="isRemoving"
            @click="cancel"
          ></button>
        </div>

        <div class="modal-body">
          <p class="mb-0">
            Are you sure you want to remove
            <strong>{{ collaborator.name }}</strong> as a collaborator?
          </p>
        </div>

        <div class="modal-footer">
          <button
            type="button"
            class="btn btn-outline-secondary btn-sm"
            :disabled="isRemoving"
            @click="cancel"
          >
            Cancel
          </button>

          <button
            type="button"
            class="btn btn-danger btn-sm"
            :disabled="isRemoving"
            @click="confirmRemoveCollaborator"
          >
            <span
              v-if="isRemoving"
              class="spinner-border spinner-border-sm me-1"
              role="status"
            ></span>
            Remove
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { apiPaths, getBearerHeaders } from '@/config/apiPaths'
import { useNotificationsStore } from '@/store/notifications'

const props = defineProps({
  isOpen: {
    type: Boolean,
    required: true,
  },
  collaborator: {
    type: Object,
    default: null,
  },
  negotiationId: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['update:isOpen', 'collaborator-removed'])

const notifications = useNotificationsStore()
const isRemoving = ref(false)

function cancel() {
  if (!isRemoving.value) {
    emit('update:isOpen', false)
  }
}

async function confirmRemoveCollaborator() {
  if (!props.collaborator) return

  isRemoving.value = true

  try {
    await axios.delete(
      `${apiPaths.NEGOTIATION_PATH}/${props.negotiationId}/collaborators/${props.collaborator.id}`,
      { headers: getBearerHeaders() },
    )

    notifications.setNotification(`${props.collaborator.name} has been removed as a collaborator.`)

    emit('collaborator-removed', props.collaborator)
    emit('update:isOpen', false)
  } catch (error) {
    notifications.setNotification(
      'Failed to remove collaborator: ' + (error.response?.data?.message ?? error.message),
    )
  } finally {
    isRemoving.value = false
  }
}
</script>
