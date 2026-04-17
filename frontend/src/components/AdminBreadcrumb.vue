<template>
  <nav aria-label="breadcrumb" class="mb-4">
    <ol class="breadcrumb mb-0">
      <li
        v-for="(segment, index) in segments"
        :key="`${segment.label}-${index}`"
        class="breadcrumb-item"
        :class="{ active: index === lastIndex }"
        :aria-current="index === lastIndex ? 'page' : undefined"
      >
        <RouterLink v-if="segment.to && index !== lastIndex" :to="segment.to">
          {{ segment.label }}
        </RouterLink>
        <span v-else>{{ segment.label }}</span>
      </li>
    </ol>
  </nav>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  segments: {
    type: Array,
    required: true,
  },
})

const lastIndex = computed(() => props.segments.length - 1)
</script>
