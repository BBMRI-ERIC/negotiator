<template>
  <VueTourDefault v-if="isVueTourDefaultVisible" />
  <NavTour
    v-if="vueTourStore.isNavTourActive"
    :isAdmin="isAdmin"
    :isResearcher="isResearcher"
    :isRepresentative="isRepresentative"
    :isNetworksTabDisplayed="isNetworksTabDisplayed"
  />
  <FilterSortTour v-if="vueTourStore.isFilterSortTourActive && vueTourStore.isFilterSortVisible" />
  <NegotiationTour v-if="vueTourStore.isNegotiationTourActive" />
  <GovernanceTour v-if="vueTourStore.isGovernanceTourActive" />
</template>
<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'

import { useUserStore } from '../../store/user'
import { useVueTourStore } from '../../store/vueTour'
import { ROLES } from '@/config/consts.js'
import VueTourDefault from '../vue-tours/VueTourDefault.vue'
import NavTour from './NavTour.vue'
import FilterSortTour from './FilterSortTour.vue'
import NegotiationTour from './NegotiationTour.vue'
import GovernanceTour from './GovernanceTour.vue'

const userStore = useUserStore()
const vueTourStore = useVueTourStore()
const roles = ref([])
const route = useRoute()

const userInfo = computed(() => {
  return userStore.userInfo
})

const isVueTourDefaultVisible = computed(() => {
  return (
    (route.fullPath === '/researcher' ||
      route.fullPath === '/admin' ||
      route.fullPath === '/biobanker') &&
    !localStorage.getItem('vue-tour-default-1')
  )
})

watch(userInfo, () => {
  retrieveUserRoles()
})

const isNetworksTabDisplayed = ref(false)

function retrieveUserRoles() {
  roles.value = userInfo.value.roles || []
  if (userInfo.value?._links.networks !== undefined) {
    isNetworksTabDisplayed.value = true
  }
}

const isAdmin = computed(() => {
  return roles.value.includes(ROLES.ADMINISTRATOR)
})
const isResearcher = computed(() => {
  return roles.value.includes(ROLES.RESEARCHER)
})
const isRepresentative = computed(() => {
  return roles.value.includes(ROLES.REPRESENTATIVE)
})
</script>
