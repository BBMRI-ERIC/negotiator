<template>
  <!-- <VueTourDefault /> -->
  <MenuTour
    :isAdmin="isAdmin"
    :isResearcher="isResearcher"
    :isRepresentative="isRepresentative"
    :isNetworksTabDisplayed="isNetworksTabDisplayed"
  />
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import VueTourDefault from '../vue-tours/VueTourDefault.vue'
import MenuTour from '../vue-tours/MenuTour.vue'
import { useUserStore } from '../../store/user'
import { ROLES } from '@/config/consts.js'

const userStore = useUserStore()
const roles = ref([])

const userInfo = computed(() => {
  return userStore.userInfo
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
