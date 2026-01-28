<template>
  <span class="badge rounded-pill" :class="badgeClass" :style="badgeStyle">
    <i v-if="icon" :class="[icon, 'px-1']" />
    <slot />
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  class: {
    type: String,
    default: '', // e.g. 'bg-success', 'bg-danger', or custom
  },
  icon: {
    type: String,
    default: '',
  },
  width: {
    type: [String, Number],
    default: '',
  },
  style: {
    type: Object,
    default: () => ({}),
  },
})

const badgeClass = computed(() => {
  return [props.class]
})

const badgeStyle = computed(() => {
  const style = { ...props.style }
  if (props.width) style.width = typeof props.width === 'number' ? props.width + 'px' : props.width
  return style
})
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  min-height: 22px;
  line-height: 1;
  padding-top: 0;
  padding-bottom: 0;
}

.badge i {
  font-size: 1em;
  line-height: 1;
  margin-top: 0;
  margin-bottom: 0;
  display: flex;
  align-items: center;
}
</style>
