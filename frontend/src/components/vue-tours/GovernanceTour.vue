<template>
  <VueTour :tourName="'governanceTour'" :steps="steps" :isStoreToLocalStorage="false" />
</template>

<script setup>
import { useTourSteps } from '@/composables/useTourSteps'
import VueTour from '../vue-tours/VueTour.vue'

const steps = useTourSteps('governanceTour')
</script>
