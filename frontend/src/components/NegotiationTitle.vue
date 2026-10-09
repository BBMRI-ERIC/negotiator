<template>
  <span ref="titleElement">{{ displayTitle }}</span>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Tooltip } from 'bootstrap'
import { DEFAULT_NEGOTIATION_TITLE_MAX_LENGTH } from '@/config/consts.js'

const props = defineProps({
  title: { type: String, default: '' },
  maxLength: { type: Number, default: DEFAULT_NEGOTIATION_TITLE_MAX_LENGTH },
})

const titleElement = ref(null)
let tooltip = null

const isTruncated = computed(() => props.maxLength > 0 && props.title.length > props.maxLength)

const displayTitle = computed(() => {
  if (!props.title) return 'Untitled'
  return isTruncated.value
    ? props.title.substring(0, props.maxLength).trimEnd() + '...'
    : props.title
})

function updateTooltip() {
  tooltip?.dispose()
  tooltip = isTruncated.value ? new Tooltip(titleElement.value, { title: () => props.title }) : null
}

onMounted(updateTooltip)
watch(isTruncated, updateTooltip)
onBeforeUnmount(() => tooltip?.dispose())
</script>
