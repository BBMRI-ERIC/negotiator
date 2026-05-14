<template>
  <div class="admin-settings-page-header mb-2">
    <div class="d-flex justify-content-between align-items-center mb-1">
      <h2 class="text-left mb-0">{{ title }}</h2>
      <div class="header-actions">
        <slot name="actions" />
      </div>
    </div>
    <hr class="my-2" />

    <div v-if="hasDescription" class="text-muted mb-3">
      <slot name="description" />
    </div>
  </div>
</template>

<script setup>
import { computed, useSlots } from 'vue'

defineProps({
  title: {
    type: String,
    required: true,
  },
})

const slots = useSlots()
const hasDescription = computed(() => Boolean(slots.description))
</script>
