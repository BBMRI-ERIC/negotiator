<template>
  <div class="form-navigation-buttons mt-auto d-flex">
    <div class="col">
      <button
        class="btn btn-sm btn-outline-info me-3 col"
        :disabled="saveDraftDisabled"
        @click="$emit('saveDraft')"
      >
        <i class="bi bi-floppy" /> Save Draft
      </button>
    </div>
    <div class="middle-buttons d-flex flex-row col">
      <button
        :class="activeNavItemIndex > 0 ? '' : 'disabled'"
        label="Previous"
        class="btn btn-sm btn-outline-info me-3"
        @click="previousTab"
      >
        Back
        <i class="bi bi-chevron-left" />
      </button>
      <button
        :class="activeNavItemIndex < navItemsLength + 1 > 0 ? '' : 'disabled'"
        class="btn btn-sm btn-outline-info me-3"
        @click="nextTab"
      >
        Next
        <i class="bi bi-chevron-right" />
      </button>
    </div>
    <div class="col">
      <button
        v-if="activeNavItemIndex == navItemsLength + 1"
        class="btn btn-sm btn-outline-info me-3 float-end"
        @click="$emit('openSaveNegotiationModal')"
      >
        <i class="bi bi-floppy" />
        Submit
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
