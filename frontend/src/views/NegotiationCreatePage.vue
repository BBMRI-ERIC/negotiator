<template>
  <negotiation-form :request-id="$route.params.requestId" :step="$route.params.step"/>
</template>

<script setup>
import { onMounted } from 'vue'
import NegotiationForm from '@/components/NegotiationForm.vue'
import { useUserStore } from '../store/user.js'

const userStore = useUserStore()

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})
</script>

<style scoped>
.box {
  inline-size: 300px;
}

header {
  line-height: 1.5;
  max-height: 100vh;
}

@media (min-width: 1024px) {
  header {
    display: flex;
    place-items: center;
    padding-right: calc(var(--section-gap) / 2);
  }

  header .wrapper {
    display: flex;
    place-items: flex-start;
    flex-wrap: wrap;
  }
}
</style>
