<template>
  <NegotiatorModal
      :id="id"
      :title="title"
  >
    <template #body>
      <p>
        {{ text }}
      </p>
      <textarea
          v-if="messageEnabled"
          v-model="message"
          class="form-control"
      />
    </template>
    <template #footer>
      <button
          type="button"
          class="btn btn-dark"
          data-bs-dismiss="modal"
          @click="message = ''"
      >
        Cancel
      </button>
      <button
          type="button"
          class="btn btn-danger"
          data-bs-dismiss="modal"
          @click="emitConfirm"
      >
        Confirm
      </button>
    </template>
  </NegotiatorModal>
</template>

<script setup>
import NegotiatorModal from "./NegotiatorModal.vue"
import {ref} from "vue"

defineProps({
  id: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  text: {
    type: String,
    required: true
  },
  messageEnabled: {
    type: Boolean,
    required: false
  }
})

const message = ref("")

const emit = defineEmits(["confirm"])

function emitConfirm() {
  emit("confirm", message.value)
}
</script>
