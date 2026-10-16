<template>
  <div>
    <div
      ref="textElement"
      :class="['ui-truncated-text', { 'ui-truncated-text--clamped': !expanded }]"
      :style="{ WebkitLineClamp: lines }"
    >
      <slot />
    </div>
    <button
      v-if="expanded || isOverflowing"
      type="button"
      class="btn btn-link btn-sm p-0"
      :aria-expanded="expanded"
      @click="expanded = !expanded"
    >
      {{ expanded ? 'Show less' : 'Show more' }}
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useResizeObserver } from '@vueuse/core'
import { TRUNCATED_TEXT_LINES } from '@/config/consts.js'

defineProps({
  lines: { type: Number, default: TRUNCATED_TEXT_LINES },
})

const textElement = ref(null)
const expanded = ref(false)
const isOverflowing = ref(false)

function measureOverflow() {
  if (expanded.value) return
  isOverflowing.value = textElement.value.scrollHeight > textElement.value.clientHeight
}

useResizeObserver(textElement, measureOverflow)
</script>

<style scoped>
.ui-truncated-text--clamped {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
