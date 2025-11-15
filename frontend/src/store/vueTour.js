import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useVueTourStore = defineStore('vueTour', () => {
  const isDefaultTourActive = ref(true)
  const isNavTourActive = ref(false)
  const isFilterSortTourActive = ref(false)
  const isFilterSortVisible = ref(false)
  const isNegotiationTourActive = ref(false)
  const isNegotiationVisible = ref(false)
  const isGovernanceTourActive = ref(false)
  const isAdminSettingsTourActive = ref(false)
  const isSettingsVisible = ref(false)

  return {
    isDefaultTourActive,
    isNavTourActive,
    isFilterSortTourActive,
    isFilterSortVisible,
    isNegotiationTourActive,
    isNegotiationVisible,
    isGovernanceTourActive,
    isAdminSettingsTourActive,
    isSettingsVisible,
  }
})
