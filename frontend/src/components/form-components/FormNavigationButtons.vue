<template>
  <div class="form-navigation-buttons mt-5 d-flex align-items-center">
    <div class="col">
      <button
        class="btn btn-outline-secondary"
        :disabled="saveDraftDisabled"
        @click="$emit('saveDraft')"
      >
        <i class="bi bi-floppy"></i> Save Draft
      </button>
    </div>
    <div class="middle-buttons d-flex flex-row col justify-content-center gap-2">
      <button
        :class="activeNavItemIndex > 0 ? '' : 'disabled'"
        class="btn btn-outline-primary"
        @click="previousTab"
      >
        <i class="bi bi-chevron-left"></i> Back
      </button>
      <button
        :class="activeNavItemIndex < navItemsLength + 1 > 0 ? '' : 'disabled'"
        class="btn btn-outline-primary"
        @click="nextTab"
      >
        Next <i class="bi bi-chevron-right"></i>
      </button>
    </div>
    <div class="col d-flex justify-content-end">
      <button
        v-if="activeNavItemIndex == navItemsLength + 1"
        class="btn btn-success"
        @click="$emit('openSaveNegotiationModal')"
      >
        <i class="bi bi-check-circle"></i> Submit Request
      </button>
    </div>
  </div>
</template>

<script setup>
const activeNavItemIndex = defineModel('activeNavItemIndex')

defineProps({
  navItemsLength: {
    type: Number,
    required: true,
    default: 0,
  },
  saveDraftDisabled: {
    type: Boolean,
    required: false,
    default: false,
  },
})

function nextTab() {
  activeNavItemIndex.value += 1
}

function previousTab() {
  activeNavItemIndex.value -= 1
}
</script>

