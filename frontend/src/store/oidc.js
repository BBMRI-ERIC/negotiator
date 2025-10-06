import { defineStore } from 'pinia'
import { piniaOidcCreateStoreModule } from 'pinia-oidc'
import oidcSettings from '../config/oidc'

// Create the OIDC store module
const oidcStoreModule = piniaOidcCreateStoreModule(
  oidcSettings,
  // Optional OIDC store settings
  { removeUserWhenTokensExpire: false },
)

// Define the Pinia store using the OIDC store module
export const useOidcStore = defineStore('oidc', oidcStoreModule)
