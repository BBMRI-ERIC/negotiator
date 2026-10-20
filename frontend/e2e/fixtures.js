import { test as base } from '@playwright/test'
import { USERS, authFile } from './users.js'
import { NegotiatorApi } from './api.js'

async function pageAs(browser, role, use) {
  const context = await browser.newContext({ storageState: authFile(role) })
  await use(await context.newPage())
  await context.close()
}

// One fixture per role, each in its own browser context, so a test can act as several users.
// `secondAdminPage` is the same admin in a separate browser, e.g. a second admin or a second tab.
export const test = base.extend({
  researcherPage: ({ browser }, use) => pageAs(browser, 'researcher', use),
  researcher2Page: ({ browser }, use) => pageAs(browser, 'researcher2', use),
  biobankerPage: ({ browser }, use) => pageAs(browser, 'biobanker', use),
  adminPage: ({ browser }, use) => pageAs(browser, 'admin', use),
  secondAdminPage: ({ browser }, use) => pageAs(browser, 'admin', use),
  api: async ({ baseURL }, use) => {
    const api = await NegotiatorApi.create(baseURL, Object.keys(USERS))
    await use(api)
    await api.dispose()
  },
})

export { expect } from '@playwright/test'
