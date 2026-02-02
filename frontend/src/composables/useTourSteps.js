import { ref } from 'vue'

export function useTourSteps(tourName) {
  const tourSteps = {
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
  }
  return ref(tourSteps[tourName] || [])
}
