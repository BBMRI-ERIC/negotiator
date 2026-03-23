const devSettings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  resource: import.meta.env.VITE_OIDC_RESOURCE,
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI,
  scope: import.meta.env.VITE_OIDC_SCOPE,
  post_logout_redirect_uri: import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI,
  response_type: import.meta.env.VITE_OIDC_RESPONSE_TYPE,
  automaticSilentRenew: import.meta.env.VITE_OIDC_AUTOMATIC_SILENT_RENEW,
  silentRedirectUri: 'http://localhost:8080/silent-renew-oidc.html',
}

const prodSettings = {
  authority: 'AUTH_URL_PLACEHOLDER',
  client_id: 'CLIENT_ID_PLACEHOLDER',
  resource: 'RESOURCES_PLACEHOLDER',
  redirect_uri: 'REDIRECT_URI_PLACEHOLDER',
  scope: 'SCOPES_PLACEHOLDER',
  post_logout_redirect_uri: 'LOGOUT_URI_PLACEHOLDER',
  response_type: 'code',
  automaticSilentRenew: true,
  silentRedirectUri: 'SILENT_REDIRECT_URI_PLACEHOLDER',
}

let oidcSettings

if (import.meta.env.DEV) {
  oidcSettings = devSettings
} else {
  oidcSettings = prodSettings
}

export default oidcSettings
