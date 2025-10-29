<template>
  <VueTour
    v-if="isAdmin || isResearcher || isRepresentative"
    :tourName="'navTour'"
    :steps="steps"
    :isStoreToLocalStorage="false"
  />
</template>

<script setup>
import { ref } from 'vue'
import VueTour from '../vue-tours/VueTour.vue'

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

function initializeSteps() {
  if (props.isAdmin) {
    steps.value.push({
      target: '.v-step-10',
      header: {
        title: 'Review Requests',
      },
      content:
        'Navigate to a page where you can view and manage access requests requiring your attention.',
    })
  }

  if (props.isResearcher) {
    steps.value.push({
      target: '.v-step-11',
      header: {
        title: 'Your Negotiations',
      },
      content:
        'Displays a list of all negotiations you are currently involved in or have been involved in previously.',
    })
  }

  if (props.isRepresentative) {
    steps.value.push({
      target: '.v-step-12',
      header: {
        title: 'Biobanker',
      },
      content:
        'Provides tools and resources for biobank representatives to manage and contribute data effectively.',
    })
  }

  if (props.isRepresentative || props.isAdmin) {
    steps.value.push({
      target: '.v-step-13',
      header: {
        title: 'Governance',
      },
      content: 'Access documents and information regarding the platforms rules and policies.',
    })
  }

  if (props.isNetworksTabDisplayed) {
    steps.value.push({
      target: '.v-step-14',
      header: {
        title: 'Networks',
      },
      content: 'Access and manage your networks within the platform.',
    })
  }

  steps.value.push({
    target: '.v-step-15',
    header: {
      title: 'Support',
    },
    content: 'Find help resources or ways to contact the support team if you encounter issues.',
  })

  steps.value.push({
    target: '.v-step-16',
    header: {
      title: 'Notifications',
    },
    content:
      'Stay updated with real-time alerts about important events, updates, or actions that require your attention on the platform.',
  })

  steps.value.push({
    target: '.v-step-17',
    header: {
      title: 'User Avatar',
    },
    content:
      'The blue circle displays your initials. Clicking on this will typically open a drop-down menu with options to view your profile, manage account settings, or log out.',
  })
}

//  first step is important for before hook to resolve user roles
const steps = ref([
  {
    target: '#v-step-0',
    header: {
      title: 'Welcome',
    },
    content:
      'The navigation bar at the top of the screen is your primary tool for getting around the platform. Its divided into two main sections: the Main Menu on the left and your User and System Information on the right.',
    before: (type) =>
      new Promise((resolve) => {
        initializeSteps()
        resolve('foo')
      }),
  },
])
</script>
