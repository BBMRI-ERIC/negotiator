<template>
  <span>{{ displayTitle }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
})

const displayTitle = computed(() => (props.title ? props.title : 'Untitled'))
</script>
