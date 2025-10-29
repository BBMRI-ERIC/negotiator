import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useVueTourStore = defineStore('vueTour', () => {
  const isNavTourActive = ref(false)
  const isFilterSortTourActive = ref(false)
  const isFilterSortVisible = ref(false)

  return {
    isNavTourActive,
    isFilterSortTourActive,
    isFilterSortVisible,
  }
})
