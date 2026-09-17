import { UserManager, WebStorageStateStore } from 'oidc-client-ts'
import oidcSettings from '../config/oidc'

export function createUserManager() {
  const { silentRedirectUri, ...settings } = oidcSettings

  return new UserManager({
    ...settings,
    silent_redirect_uri: silentRedirectUri,
    userStore: new WebStorageStateStore({ store: window.localStorage }),
  })
}

export const userManager = createUserManager()
