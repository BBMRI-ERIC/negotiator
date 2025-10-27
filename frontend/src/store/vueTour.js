import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useVueTourStore = defineStore('vueTour', () => {
  const isNavTourActive = ref(false)


  return {
    isNavTourActive,

  }
})
