<template>
  <div
      :id="id"
      class="modal"
      :class="{ fade: fade }"
      tabindex="-1"
      :aria-labelledby="`${id}Label`"
      aria-hidden="true"
  >
    <div
        class="modal-dialog modal-dialog-centered"
        :class="isModalSmall === true ? 'modal-sm' : ''"
    >
      <div class="modal-content text-center">
        <div class="modal-header justify-content-center">
          <h4 class="modal-title ">
            {{ title }}
          </h4>
          <button
              v-if="isXButtondisplayed"
              type="button"
              class="btn-close"
              data-bs-dismiss="modal"
              aria-label="Close"
          />
        </div>

        <div class="modal-body">
          <slot name="body"/>
        </div>
        <div class="modal-footer justify-content-center">
          <slot name="footer"/>
        </div>
      </div>
    </div>
  </div>
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
  fade: {
    type: Boolean,
    required: false,
    default: true
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
</script>
