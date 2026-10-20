import { test as setup, expect } from '@playwright/test'
import { USERS, authFile } from './users.js'

// Logs every test user in once through the OIDC mock and stores the session (pinia-oidc keeps
// the user in localStorage), so specs start authenticated instead of typing credentials.
for (const [role, { username, password }] of Object.entries(USERS)) {
  setup(`authenticate as ${role}`, async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: 'Life Science Login' }).click()
    await page.locator('#Input_Username').fill(username)
    await page.locator('#Input_Password').fill(password)
    await page.getByRole('button', { name: 'Login' }).click()
    await expect(page).toHaveURL(/\/researcher/)
    await page.context().storageState({ path: authFile(role) })
  })
}
