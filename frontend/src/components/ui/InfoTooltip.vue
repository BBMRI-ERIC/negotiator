<template>
  <button
    ref="trigger"
    type="button"
    class="info-tooltip p-0 border-0 bg-transparent"
    aria-label="More information"
  >
    <i class="py-1 bi bi-info-circle" aria-hidden="true" />
  </button>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Tooltip } from 'bootstrap'

const props = defineProps({
  text: {
    type: String,
    required: true,
  },
})

const trigger = ref(null)
let tooltip = null

onMounted(() => {
  // title as a function so the tooltip always shows the current prop value
  tooltip = new Tooltip(trigger.value, { title: () => props.text })
})

onBeforeUnmount(() => {
  tooltip?.dispose()
})
</script>

<style scoped>
.info-tooltip {
  color: inherit;
}
</style>
