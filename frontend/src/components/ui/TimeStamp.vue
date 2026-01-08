<template>
  <span
    :class="['ui-timestamp', { 'ui-timestamp--muted': muted }, customClass]"
    :style="customStyle"
    :title="fullDate"
  >
    {{ formattedDate }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { formatTimestampToLocalDateTime } from '@/composables/utils'

const props = defineProps({
  value: {
    type: [String, Number, Date],
    required: true,
  },
  class: {
    type: String,
    default: '',
  },
  style: {
    type: Object,
    default: () => ({}),
  },
  format: {
    type: String,
    default: '', // Optionally allow custom format in the future
  },
  muted: {
    type: Boolean,
    default: false,
  },
})

const formattedDate = computed(() => {
  // You can extend this to support custom formats if needed
  return props.value ? formatTimestampToLocalDateTime(props.value) : 'Unknown date'
})

const fullDate = computed(() => {
  if (!props.value) return ''
  const date = new Date(props.value)
  return date.toLocaleString()
})

const customClass = computed(() => props.class)
const customStyle = computed(() => props.style)
</script>

<style scoped>
.ui-timestamp {
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.ui-timestamp--muted {
  color: #3c3c3d;
  opacity: 0.7;
}
</style>
