import { test, expect } from '../fixtures.js'

// "Type 2 Diabetes Research Initiative" from the dev seed data: created by Researcher,
// in progress, with a resource represented by Biobanker.
const NEGOTIATION_ID = '550e8400-e29b-41d4-a716-446655440001'

test('researcher sends a public message that the representative can read', async ({
  researcherPage,
  biobankerPage,
}) => {
  const message = `Playwright message ${Date.now()}`

  await researcherPage.goto(`/negotiations/${NEGOTIATION_ID}/ROLE_RESEARCHER`)
  await researcherPage.locator('textarea.mb-3').fill(message)
  await researcherPage.locator('#recipient').selectOption('public')
  await researcherPage.locator('#send').click()
  await expect(researcherPage.getByText(message)).toBeVisible()

  await biobankerPage.goto(`/negotiations/${NEGOTIATION_ID}/ROLE_REPRESENTATIVE`)
  await expect(biobankerPage.getByText(message)).toBeVisible()
})
