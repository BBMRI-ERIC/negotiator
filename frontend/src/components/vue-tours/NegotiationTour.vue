<template>
  <VueTour :tourName="'negotiationTour'" :steps="steps" :isStoreToLocalStorage="false" />
</template>

<script setup>
import { ref } from 'vue'
import VueTour from '../vue-tours/VueTour.vue'
import { useVueTourStore } from '@/store/vueTour.js'

const vueTourStore = useVueTourStore()

const steps = ref([
  {
    target: '#v-step-0',
    header: {
      title: 'Negotiation tour',
    },
    content:
      'Click on a negotiation from the list to continue the tour. This tour will guide you through the key features of the negotiation view.',
  },
  {
    target: '.v-step-negotiation-0',
    header: {
      title: 'Negotiation title',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'This is the title of the negotiation you have selected. It provides a brief overview of the negotiation topic.',
    before: () =>
      new Promise((resolve) => {
        if (vueTourStore.isNegotiationVisible) {
          resolve('foo')
        }
      }),
  },
  {
    target: '.v-step-negotiation-1',
    header: {
      title: 'Negotiation body',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'Here you can view the main content of the negotiation, including all relevant details and information.',
  },
  {
    target: '.v-step-negotiation-3',
    header: {
      title: 'Negotiation author',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'This section displays information about the author of the negotiation, including their name and contact details.',
  },
  {
    target: '.v-step-negotiation-4',
    header: {
      title: 'Negotiation id',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'The unique identifier for this negotiation. It is used for tracking and reference purposes.',
  },
  {
    target: '.v-step-negotiation-5',
    header: {
      title: 'Negotiation submission date',
    },
    params: {
      enableScrolling: false,
    },
    content: 'This indicates when the negotiation was initially submitted.',
  },
  {
    target: '.v-step-negotiation-6',
    header: {
      title: 'Negotiation status',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'This shows the current status of the negotiation, such as Draft, Submitted, or Closed.',
  },
  {
    target: '.v-step-negotiation-7',
    header: {
      title: 'Manage negotiation actions',
    },
    params: {
      placement: 'bottom',
      enableScrolling: false,
    },
    content:
      'From here, you can manage various actions related to the negotiation, such as updating its status or deleting it if applicable.',
  },
  {
    target: '.v-step-negotiation-8',
    header: {
      title: 'Download negotiation data',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'You can download all relevant data associated with this negotiation for offline review or record-keeping purposes.',
  },
  {
    target: '.v-step-negotiation-9',
    header: {
      title: 'Messaging panel',
    },

    content:
      'This panel allows you to communicate with other parties involved in the negotiation. You can send and receive messages here.',
  },
])
</script>
