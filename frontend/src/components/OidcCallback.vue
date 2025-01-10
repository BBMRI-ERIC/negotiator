<template>
  <div />
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOidcStore } from '../store/oidc'

const oidcStore = useOidcStore()
const router = useRouter()

onMounted(() => {
  oidcSignInCallback()
})

async function oidcSignInCallback() {
  await oidcStore
    .oidcSignInCallback()
    .then((redirectPath) => {
      router.push(redirectPath)
    })
    .catch((err) => {
      console.error(err)
      router.push('/oidc-callback-error') // Handle errors any way you want
    })
}
</script>
