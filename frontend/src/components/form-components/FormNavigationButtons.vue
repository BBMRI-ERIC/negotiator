<template>
  <div class="form-navigation-buttons mt-auto d-flex">
    <div class="col">
      <PrimaryButton
        :isDisabled="saveDraftDisabled"
        size="sm"
        class="me-3 col"
        @click="$emit('saveDraft')"
      >
        <i class="bi bi-floppy" /> Save Draft
      </PrimaryButton>
    </div>
    <div class="middle-buttons d-flex flex-row col">
      <PrimaryButton
        :isDisabled="activeNavItemIndex <= 0"
        size="sm"
        class="me-3"
        @click="previousTab"
      >
        Back
        <i class="bi bi-chevron-left" />
      </PrimaryButton>
      <PrimaryButton
        :isDisabled="activeNavItemIndex >= navItemsLength + 1"
        size="sm"
        class="me-3"
        @click="nextTab"
      >
        Next
        <i class="bi bi-chevron-right" />
      </PrimaryButton>
    </div>
    <div class="col">
      <PrimaryButton
        v-if="activeNavItemIndex == navItemsLength + 1"
        size="sm"
        class="me-3 float-end"
        @click="$emit('openSaveNegotiationModal')"
      >
        <i class="bi bi-floppy" />
        Submit
      </PrimaryButton>
    </div>
  </div>
</template>

<script setup>
import PrimaryButton from '@/components/ui/buttons/PrimaryButton.vue'
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
