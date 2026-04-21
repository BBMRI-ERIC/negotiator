<template>
  <span class="badge rounded-pill d-inline-flex align-items-center gap-1" :class="badgeClass">
    <span>{{ statusText }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  delivery: {
    type: Object,
    required: true,
  },
})

const statusCode = computed(() => props.delivery?.httpStatusCode)

const hasStatusCode = computed(() => statusCode.value !== null && statusCode.value !== undefined)

const isOk = computed(() => {
  const parsedStatusCode = Number(statusCode.value)
  return Number.isInteger(parsedStatusCode) && parsedStatusCode >= 200 && parsedStatusCode <= 299
})

const badgeClass = computed(() => (isOk.value ? 'bg-success' : 'bg-danger'))

const statusText = computed(() => {
  if (hasStatusCode.value) {
    return String(statusCode.value)
  }

  // Error message, but no status code
  if (props.delivery?.errorMessage) {
    return 'none'
  }

  return ''
})
</script>
