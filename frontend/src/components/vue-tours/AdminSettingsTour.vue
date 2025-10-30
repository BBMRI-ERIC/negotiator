<template>
  <VueTour :tourName="'adminSettingsTour'" :steps="steps" :isStoreToLocalStorage="false" />
</template>

<script setup>
import { ref } from 'vue'
import VueTour from '../vue-tours/VueTour.vue'

const steps = ref([
  {
    target: '.v-step-settings-0',
    header: {
      title: 'Administrator Console',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'The Admin Settings Page is a centralized interface for administrators to configure and manage system-wide settings. This page provides tools to customize application behavior, manage user roles, and ensure the system operates according to organizational requirements.',
  },
  {
    target: '.v-step-settings-1',
    header: {
      title: 'Information Requirements',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'The Negotiator allows you to define additional information requirements for Resource State Machine transitions available to the Representatives.',
  },
  {
    target: '.v-step-settings-2',
    header: {
      title: 'Webhooks',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'The Negotiator uses a webhook system to allow external systems to be notified of events that occur within the Negotiator. This guide provides an overview of how to set up and use webhooks with the Negotiator.',
  },
  {
    target: '.v-step-settings-3',
    header: {
      title: 'Emails',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'Search and manage all email templates used by the Negotiator for various notifications and communications with users.',
  },
  {
    target: '.v-step-settings-4',
    header: {
      title: 'Users',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'Search user accounts, roles, and permissions within the Negotiator system to ensure proper access control and security.',
  },
  {
    target: '.v-step-settings-5',
    header: {
      title: 'Templates',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'The Negotiator provides a set of customizable templates used for generating PDFs, emails, and more',
  },
  {
    target: '.v-step-settings-6',
    header: {
      title: 'Access Forms',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'Adding and managing access forms that users must complete to request access to resources within the Negotiator platform.',
  },
  {
    target: '.v-step-settings-7',
    header: {
      title: 'Form Elements',
    },
    params: {
      enableScrolling: false,
    },
    content:
      'Create and customize form elements that can be used in access forms throughout the Negotiator system.',
  },
])
</script>
