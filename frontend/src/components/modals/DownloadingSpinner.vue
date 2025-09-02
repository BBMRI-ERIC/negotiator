<template>
  <div
    class="modal fade"
    ref="spinnerModal"
    tabindex="-1"
    aria-hidden="true"
    data-bs-backdrop="static"
    data-bs-keyboard="true"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content border-0 shadow-none">
        <div class="d-flex justify-content-center flex-row p-5">
          <div class="d-flex justify-content-center">
            <div class="spinner-border d-flex justify-content-center" role="status" />
            <div class="d-flex justify-content-center">
              <h4 class="mb-3 ms-3">Downloading ...</h4>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['update:modelValue'])

const spinnerModal = ref(null)

</script>