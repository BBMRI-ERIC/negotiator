<template>
  <button
    :class="computedClass"
    :style="computedStyle"
    :type="type"
    :disabled="disabled"
    @click="onClick"
    v-bind="$attrs"
  >
    <slot name="icon" />
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'button' },
  color: { type: String, default: '' },
  active: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  dropdown: { type: Boolean, default: false },
  size: { type: String, default: 'sm' },
  customClass: { type: String, default: '' },
  customStyle: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['click'])

const computedClass = computed(() => {
  return [
    'btn',
    `btn-${props.size}`,
    props.dropdown ? 'dropdown-toggle' : '',
    props.active ? 'show' : '',
    props.customClass,
  ]
    .filter(Boolean)
    .join(' ')
})

const computedStyle = computed(() => {
  return {
    ...props.customStyle,
    ...(props.color ? { 'background-color': props.color, borderColor: props.color } : {}),
  }
})

function onClick(event: MouseEvent) {
  if (!props.disabled) emit('click', event)
}
</script>

<style scoped>
.btn {
  transition:
    background 0.2s,
    color 0.2s;
}
</style>
