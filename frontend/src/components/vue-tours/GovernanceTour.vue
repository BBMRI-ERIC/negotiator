<template>
  <VueTour :tourName="'governanceTour'" :steps="steps" :isStoreToLocalStorage="false" />
</template>

<script setup>
import { ref } from 'vue'
import VueTour from '../vue-tours/VueTour.vue'

const steps = ref([
  {
    target: '.v-step-governance-0',
    header: {
      title: 'Organizations',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'Menage and view details about the organizations and resources you are affiliated with in the Governance section.',
  },
  {
    target: '.v-step-governance-1',
    header: {
      title: 'Networks Management',
    },
    params: {
      enableScrolling: false,
    },
    content: 'Manage networks and their connections to resources.',
  },
  {
    target: '.v-step-governance-2',
    header: {
      title: 'Organization search',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'Here you can search for specific organizations within the governance section by entering relevant keywords or names.',
  },
  {
    target: '.v-step-governance-3',
    header: {
      title: 'Create new organization',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'Click this button to initiate the process of creating a new organization within the governance framework.',
  },
])
</script>
