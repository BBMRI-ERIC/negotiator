<template>
  <VueTour :tourName="'vue_default_tour_1'" :steps="steps" :isStoreToLocalStorage="true" />
</template>

<script setup>
import { useTourSteps } from '@/composables/useTourSteps'
import VueTour from '../vue-tours/VueTour.vue'

const steps = useTourSteps('vue_default_tour_1')
</script>
