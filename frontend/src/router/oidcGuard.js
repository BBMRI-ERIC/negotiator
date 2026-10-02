export function createOidcGuard(userManager, oidcStore) {
  return async (to, from, next) => {
    try {
      await oidcStore.ready

      const callbackPath = new URL(userManager.settings.redirect_uri).pathname
      if (to.path === callbackPath) {
        return next()
      }

      if (oidcStore.oidcIsAuthenticated) {
        return next()
      }

      const canSigninSilently = !!userManager.settings.silent_redirect_uri

      if (to.meta.isPublic) {
        if (canSigninSilently) {
          userManager.signinSilent().catch(() => {})
        }
        return next()
      }

      if (canSigninSilently) {
        await userManager.signinSilent().catch(() => {})
      }

      if (oidcStore.oidcIsAuthenticated) {
        return next()
      }

      await oidcStore.authenticateOidc(to.fullPath).catch(() => next())
    } catch {
      next()
    }
  }
}
