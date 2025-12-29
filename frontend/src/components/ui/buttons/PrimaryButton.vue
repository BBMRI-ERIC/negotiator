<template>
  <div class="primary-button">
    <button
      :class="['btn', sizeClass, 'btn-outline-info', 'me-3', { disabled: isDisabled }]"
      :style="buttonStyle"
      @click="nextTab"
      @mouseover="handleMouseOver"
      @mouseout="handleMouseOut"
    >
      <slot></slot>
    </button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  isDisabled: {
    type: Boolean,
    required: false,
    default: false,
  },
  size: {
    type: String,
    default: 'sm',
    validator: (val) => ['sm', 'lg', 'md', ''].includes(val),
  },
  backgroundColor: {
    type: String,
    required: false,
    default: '',
  },
  textColor: {
    type: String,
    required: false,
    default: '',
  },
  borderColor: {
    type: String,
    required: false,
    default: '',
  },
  hoverBackgroundColor: {
    type: String,
    required: false,
    default: '',
  },
  hoverTextColor: {
    type: String,
    required: false,
    default: '',
  },
  hoverBorderColor: {
    type: String,
    required: false,
    default: '',
  },
})

const sizeClass = computed(() => {
  if (props.size === 'sm') return 'btn-sm'
  if (props.size === 'lg') return 'btn-lg'
  if (props.size === 'md' || props.size === '') return ''
  return ''
})

const currentBackgroundColor = ref(props.backgroundColor)
const currentTextColor = ref(props.textColor)
const currentBorderColor = ref(props.borderColor)

const buttonStyle = computed(() => {
  const style = {}
  if (currentBackgroundColor.value) style.backgroundColor = currentBackgroundColor.value
  if (currentTextColor.value) style.color = currentTextColor.value
  if (currentBorderColor.value) style.borderColor = currentBorderColor.value
  return style
})

function handleMouseOver() {
  if (props.hoverBackgroundColor) currentBackgroundColor.value = props.hoverBackgroundColor
  if (props.hoverTextColor) currentTextColor.value = props.hoverTextColor
  if (props.hoverBorderColor) currentBorderColor.value = props.hoverBorderColor
}

function handleMouseOut() {
  currentBackgroundColor.value = props.backgroundColor
  currentTextColor.value = props.textColor
  currentBorderColor.value = props.borderColor
}
</script>