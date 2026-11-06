<template>
  <span ref="titleElement" class="negotiation-title" :style="{ WebkitLineClamp: lines }">{{
    displayTitle
  }}</span>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Tooltip } from 'bootstrap'
import { useResizeObserver } from '@vueuse/core'
import { NEGOTIATION_TITLE_LINES } from '@/config/consts.js'

const props = defineProps({
  title: { type: String, default: '' },
  lines: { type: Number, default: NEGOTIATION_TITLE_LINES },
})

const titleElement = ref(null)
const isTruncated = ref(false)
let tooltip = null

const displayTitle = computed(() => (props.title ? props.title : 'Untitled'))

function measureOverflow() {
  isTruncated.value = titleElement.value.scrollHeight > titleElement.value.clientHeight
}

function updateTooltip() {
  tooltip?.dispose()
  tooltip = isTruncated.value ? new Tooltip(titleElement.value, { title: () => props.title }) : null
}

useResizeObserver(titleElement, measureOverflow)
watch(() => props.title, measureOverflow, { flush: 'post' })
watch(isTruncated, updateTooltip)
onBeforeUnmount(() => tooltip?.dispose())
</script>

<style scoped>
.negotiation-title {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
}
</style>
