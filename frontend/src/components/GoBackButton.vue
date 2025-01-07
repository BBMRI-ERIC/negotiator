<template>
  <button
      type="button"
      class="btn"
      :style="{ 'background-color': uiConfiguration.buttonColor, 'border-color': uiConfiguration.buttonColor, 'color': '#FFFFFF'}"
      @click="goBack()"
  >
    <i class="bi-arrow-left"/>
    Go back
  </button>
</template>

<script setup>
import {computed} from "vue"
import {useRouter} from "vue-router"
import {useUiConfiguration} from '@/store/uiConfiguration.js'

const router = useRouter()
const uiConfigurationStore = useUiConfiguration()

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

function goBack() {
  if (router.options.history.state.back) {
    router.go(-1)
  } else {
    router.push({
      name: "home"
    })
  }
}
</script>
