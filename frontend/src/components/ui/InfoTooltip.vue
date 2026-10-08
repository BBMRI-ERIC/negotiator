<template>
  <i ref="icon" class="py-1 bi bi-info-circle" />
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

const icon = ref(null)
let tooltip = null

onMounted(() => {
  // title as a function so the tooltip always shows the current prop value
  tooltip = new Tooltip(icon.value, { title: () => props.text })
})

onBeforeUnmount(() => {
  tooltip?.dispose()
})
</script>
