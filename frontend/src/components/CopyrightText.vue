<template>
  <p>&copy; {{ returnCurrentYear }} BBMRI-ERIC</p>
</template>

<script setup>
import { computed } from 'vue'
import moment from 'moment'

const returnCurrentYear = computed(() => {
  return moment().format('YYYY') || '2025'
})
</script>
