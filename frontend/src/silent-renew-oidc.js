import { createUserManager } from './services/oidcUserManager'

createUserManager()
  .signinSilentCallback()
  .catch((err) => {
    // Keep this minimal because this page only runs in a hidden iframe.
    console.error('OIDC silent renew callback failed', err)
  })
