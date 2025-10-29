import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useVueTourStore = defineStore('vueTour', () => {
  const isNavTourActive = ref(false)
  const isFilterSortTourActive = ref(false)
  const isFilterSortVisible = ref(false)
  const isNegotiationTourActive = ref(false)
  const isNegotiationVisible = ref(false)

  return {
    isNavTourActive,
    isFilterSortTourActive,
    isFilterSortVisible,
    isNegotiationTourActive,
    isNegotiationVisible,
  }
})
