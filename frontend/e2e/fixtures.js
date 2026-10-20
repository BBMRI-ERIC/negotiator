import fs from 'node:fs'
import { test as base } from '@playwright/test'
import { authFile } from './users.js'

async function pageAs(browser, role) {
  const { sessionStorage, ...storageState } = JSON.parse(fs.readFileSync(authFile(role), 'utf8'))
  const context = await browser.newContext({ storageState })
  await context.addInitScript((entries) => {
    for (const [key, value] of Object.entries(entries)) window.sessionStorage.setItem(key, value)
  }, sessionStorage)
  return context.newPage()
}

// One fixture per role, each in its own browser context, so a test can act as several users.
export const test = base.extend({
  researcherPage: async ({ browser }, use) => {
    const page = await pageAs(browser, 'researcher')
    await use(page)
    await page.context().close()
  },
  biobankerPage: async ({ browser }, use) => {
    const page = await pageAs(browser, 'biobanker')
    await use(page)
    await page.context().close()
  },
  adminPage: async ({ browser }, use) => {
    const page = await pageAs(browser, 'admin')
    await use(page)
    await page.context().close()
  },
})

export { expect } from '@playwright/test'
