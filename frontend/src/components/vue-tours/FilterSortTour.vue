<template>
  <VueTour :tourName="'filterSortTour'" :steps="steps" :isStoreToLocalStorage="false" />
</template>

<script setup>
import { ref } from 'vue'
import VueTour from '../vue-tours/VueTour.vue'

const steps = ref([
  {
    target: '.v-step-20',
    header: {
      title: 'Sorting and Filtering',
    },
    content:
      'Use the sorting and filtering options to quickly find specific negotiations based on various criteria.',
  },
  {
    target: '.v-step-21',
    header: {
      title: 'Sort by',
    },
    content:
      'Sort your negotiations by different attributes such as title, creation date, or current state.',
  },
  {
    target: '.v-step-22',
    header: {
      title: 'Sort by order',
    },
    content:
      'After selecting a sorting option, click the sort icon again to toggle between ascending and descending order.',
  },
  {
    target: '.v-step-23',
    header: {
      title: 'Filter by status',
    },
    content: 'Use the filter options to narrow down your negotiations based on their status.',
  },
  {
    target: '.v-step-24',
    header: {
      title: 'Filter by date',
    },
    content:
      'You can also filter negotiations based on a specific date range to find negotiations created within that period.',
  },
  {
    target: '.v-step-25',
    header: {
      title: 'Clear filters',
    },
    content:
      'Click the "Clear all filters" button to reset all applied filters and view the complete list of negotiations again.',
  },
])
</script>
