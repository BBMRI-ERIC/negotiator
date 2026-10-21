import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { userManager } from '../services/oidcUserManager'

const ACTIVE_ROUTE_KEY = 'oidc_active_route'

export const useOidcStore = defineStore('oidc', () => {
  const user = ref(null)

  const ready = userManager.getUser().then((loadedUser) => {
    if (loadedUser && !loadedUser.expired) {
      user.value = loadedUser
    }
  })

  userManager.events.addUserLoaded((loadedUser) => {
    user.value = loadedUser
  })
  userManager.events.addAccessTokenExpired(() => {
    user.value = null
  })

  const oidcIsAuthenticated = computed(() => !!user.value && !user.value.expired)
  const oidcUser = computed(() => user.value?.profile ?? null)
  const access_token = computed(() => user.value?.access_token ?? null)

  function authenticateOidc(redirectPath) {
    if (redirectPath) {
      sessionStorage.setItem(ACTIVE_ROUTE_KEY, redirectPath)
    } else {
      sessionStorage.removeItem(ACTIVE_ROUTE_KEY)
    }
    return userManager.signinRedirect()
  }

  function signOutOidc() {
    return userManager.signoutRedirect()
  }

  function oidcSignInCallback() {
    return userManager.signinRedirectCallback().then(() => {
      return sessionStorage.getItem(ACTIVE_ROUTE_KEY) || '/'
    })
  }

  return {
    ready,
    oidcIsAuthenticated,
    oidcUser,
    access_token,
    authenticateOidc,
    signOutOidc,
    oidcSignInCallback,
  }
})
