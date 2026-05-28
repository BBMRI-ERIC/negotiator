<template>
  <div class="admin-settings-page-header mb-3">
    <div class="d-flex justify-content-between align-items-center mb-2">
      <h2 class="text-left mb-0">{{ title }}</h2>
      <div v-if="actions" class="d-flex gap-2">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="description" class="text-muted">
      <slot name="description" />
    </div>
  </div>
</template>

<script setup>
import { useSlots } from 'vue'

defineProps({
  title: {
    type: String,
    required: true,
  },
})

const { actions, description } = useSlots()
</script>
