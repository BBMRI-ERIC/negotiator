<template>
  <i :class="iconClass" aria-hidden="true" />
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

const isOk = computed(() => {
  const parsedStatusCode = Number(statusCode.value)
  return Number.isInteger(parsedStatusCode) && parsedStatusCode >= 200 && parsedStatusCode <= 299
})

const iconClass = computed(() =>
  isOk.value ? 'bi bi-check-circle text-success' : 'bi bi-x-circle text-danger',
)
</script>
