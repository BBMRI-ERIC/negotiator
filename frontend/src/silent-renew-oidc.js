import { piniaOidcProcessSilentSignInCallback } from 'pinia-oidc'
import oidcSettings from './config/oidc'

piniaOidcProcessSilentSignInCallback(oidcSettings).catch((err) => {
  // Keep this minimal because this page only runs in a hidden iframe.
  console.error('OIDC silent renew callback failed', err)
})
