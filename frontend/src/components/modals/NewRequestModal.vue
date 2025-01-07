<template>
  <NegotiatorModal
      :id="id"
      :title="title"
      :is-modal-small="isModalSmall"
  >
    <template #body>
      <p>
        {{ text }}
      </p>
    </template>
    <template #footer>
      <button
          type="button"
          class="btn btn-secondary px-4"
          data-bs-dismiss="modal"
          @click="emitConfirm"
      >
        OK
      </button>
    </template>
  </NegotiatorModal>
</template>

<script setup>
const props = defineProps({
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
  isModalSmall: {
    type: Boolean,
    required: false,
    default: false
  },
  isXButtondisplayed: {
    type: Boolean,
    required: false,
    default: true
  }
})

const emit = defineEmits(["confirm"])

function emitConfirm() {
  emit("confirm")
}
</script>
