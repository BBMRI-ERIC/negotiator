import { ref } from 'vue'
import { useVueTourStore } from '@/store/vueTour.js'

export function useTourSteps(tourName, props = {}) {
  const vueTourStore = useVueTourStore()

  const tourSteps = {
    vue_default_tour_1: [
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
    ],
    negotiationTour: [
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
    ],
    filterSortTour: [
      {
        target: '.v-step-20',
        header: { title: 'Sorting and Filtering' },
        params: { enableScrolling: false },
        content:
          'Use the sorting and filtering options to quickly find specific negotiations based on various criteria.',
      },
      {
        target: '.v-step-21',
        header: { title: 'Sort by' },
        params: { enableScrolling: false },
        content:
          'Sort your negotiations by different attributes such as title, creation date, or current state.',
      },
      {
        target: '.v-step-22',
        header: { title: 'Sort by order' },
        params: { enableScrolling: false },
        content:
          'After selecting a sorting option, click the sort icon again to toggle between ascending and descending order.',
      },
      {
        target: '.v-step-23',
        header: { title: 'Filter by status' },
        params: { enableScrolling: false },
        content: 'Use the filter options to narrow down your negotiations based on their status.',
      },
      {
        target: '.v-step-24',
        header: { title: 'Filter by date' },
        params: { enableScrolling: false },
        content:
          'You can also filter negotiations based on a specific date range to find negotiations created within that period.',
      },
      {
        target: '.v-step-25',
        header: { title: 'Clear filters' },
        params: { enableScrolling: false },
        content:
          'Click the "Clear all filters" button to reset all applied filters and view the complete list of negotiations again.',
      },
    ],
    governanceTour: [
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
    ],
    adminSettingsTour: [
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
          enableScrolling: true,
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
          enableScrolling: true,
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
          enableScrolling: true,
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
          enableScrolling: true,
        },
        content:
          'Create and customize form elements that can be used in access forms throughout the Negotiator system.',
      },
    ],
    navTour: () => {
      const steps = [
        {
          target: '#v-step-0',
          header: { title: 'Welcome' },
          content:
            'The navigation bar at the top of the screen is your primary tool for getting around the platform. Its divided into two main sections: the Main Menu on the left and your User and System Information on the right.',
        },
      ]
      if (props.isAdmin) {
        steps.push({
          target: '.v-step-10',
          header: { title: 'Review Requests' },
          content:
            'Navigate to a page where you can view and manage access requests requiring your attention.',
        })
      }
      if (props.isResearcher) {
        steps.push({
          target: '.v-step-11',
          header: { title: 'Your Negotiations' },
          content:
            'Displays a list of all negotiations you are currently involved in or have been involved in previously.',
        })
      }
      if (props.isRepresentative) {
        steps.push({
          target: '.v-step-12',
          header: { title: 'Biobanker' },
          content:
            'Provides tools and resources for biobank representatives to manage and contribute data effectively.',
        })
      }
      if (props.isRepresentative || props.isAdmin) {
        steps.push({
          target: '.v-step-13',
          header: { title: 'Governance' },
          content: 'Access documents and information regarding the platforms rules and policies.',
        })
      }
      if (props.isNetworksTabDisplayed) {
        steps.push({
          target: '.v-step-14',
          header: { title: 'Networks' },
          content: 'Access and manage your networks within the platform.',
        })
      }
      steps.push(
        {
          target: '.v-step-15',
          header: { title: 'Support' },
          content:
            'Find help resources or ways to contact the support team if you encounter issues.',
        },
        {
          target: '.v-step-16',
          header: { title: 'Notifications' },
          content:
            'Stay updated with real-time alerts about important events, updates, or actions that require your attention on the platform.',
        },
        {
          target: '.v-step-17',
          header: { title: 'User Avatar' },
          content:
            'The blue circle displays your initials. Clicking on this will typically open a drop-down menu with options to view your profile, manage account settings, or log out.',
        },
      )
      return steps
    },
  }
  if (tourName === 'navTour' && typeof tourSteps.navTour === 'function') {
    return ref(tourSteps.navTour(props))
  }
  return ref(tourSteps[tourName] || [])
}
