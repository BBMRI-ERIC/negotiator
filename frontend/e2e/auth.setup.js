import fs from 'node:fs'
import { test as setup, expect } from '@playwright/test'
import { USERS, authFile } from './users.js'

// Logs every test user in once through the OIDC mock and stores the session, so specs start
// authenticated instead of typing credentials in every test.
for (const [role, { username, password }] of Object.entries(USERS)) {
  setup(`authenticate as ${role}`, async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: 'Life Science Login' }).click()
    await page.locator('#Input_Username').fill(username)
    await page.locator('#Input_Password').fill(password)
    await page.getByRole('button', { name: 'Login' }).click()
    await expect(page).toHaveURL(/\/researcher/)

    // oidc-client-ts keeps the user in sessionStorage, which storageState does not capture.
    const state = await page.context().storageState()
    state.sessionStorage = await page.evaluate(() => ({ ...sessionStorage }))
    fs.mkdirSync('e2e/.auth', { recursive: true })
    fs.writeFileSync(authFile(role), JSON.stringify(state))
  })
}
