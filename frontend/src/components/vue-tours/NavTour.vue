<template>
  <VueTour
    v-if="isAdmin || isResearcher || isRepresentative"
    :tourName="'navTour'"
    :steps="steps"
    :isStoreToLocalStorage="false"
  />
</template>

<script setup>
import VueTour from '../vue-tours/VueTour.vue'
import { useTourSteps } from '@/composables/useTourSteps'

const props = defineProps({
  isAdmin: {
    type: Boolean,
    required: true,
    default: false,
  },
  isResearcher: {
    type: Boolean,
    required: true,
    default: false,
  },
  isRepresentative: {
    type: Boolean,
    required: true,
    default: false,
  },
  isNetworksTabDisplayed: {
    type: Boolean,
    required: false,
    default: false,
  },
})

const steps = useTourSteps('navTour', props)
</script>
