import { defineStore } from 'pinia'
import { piniaOidcCreateStoreModule } from 'pinia-oidc'
import oidcSettings from '../config/oidc'

export const useOidcStore = defineStore(
  piniaOidcCreateStoreModule(
    oidcSettings,
    // Optional OIDC store settings
    { removeUserWhenTokensExpire: false },
  ),
)
