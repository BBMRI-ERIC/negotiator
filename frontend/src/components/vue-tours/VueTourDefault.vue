<template>
  <VueTour :tourName="'vue-tour-default-1'" :steps="steps" :isStoreToLocalStorage="true" />
</template>

<script setup>
import { ref } from 'vue'
import VueTour from '../vue-tours/VueTour.vue'

const steps = ref([
  {
    target: '#v-step-0',
    header: {
      title: 'Welcome',
    },
    content:
      'In the <strong>Negotiator</strong>, you can view the status of your negotiations and stay in contact with the providers of the desired resources.',
  },
  {
    target: '#v-step-2',
    header: {
      title: 'Status',
    },
    params: {
      placement: 'top',
    },
    content: 'You can see the current status of your enquiry at a glance.',
  },
  {
    target: '.v-step-3',
    header: {
      title: 'Filter',
    },
    content: 'You also have the option of sorting and filtering your negotiations.',
  },
])
</script>
