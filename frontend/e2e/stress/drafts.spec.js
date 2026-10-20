import { test, expect } from '../fixtures.js'
import { authFile } from '../users.js'

// These tests depend on Researcher2's list of drafts, so they run one after another in a single
// worker and leave no drafts behind.
test.describe.configure({ mode: 'default' })

test.beforeEach(({ api }) => api.deleteDrafts('researcher2'))
test.afterEach(({ api }) => api.deleteDrafts('researcher2'))

test('opening one of two drafts from today keeps both drafts', async ({
  researcher2Page: page,
  api,
}) => {
  // A researcher starts two requests from the Directory in the morning and edits one later.
  const first = await api.createNegotiation('DRAFT', 'researcher2')
  const second = await api.createNegotiation('DRAFT', 'researcher2')

  await page.goto(`/edit/requests/${first}`)
  await expect(page.getByText('Selected Resources')).toBeVisible()

  await expect(page).toHaveURL(new RegExp(`/edit/requests/${first}`))
  expect(await api.exists(first), 'the draft being edited was deleted').toBeTruthy()
  expect(await api.exists(second)).toBeTruthy()
})

test('a double-clicked submit confirmation still submits the negotiation', async ({
  researcher2Page: page,
  api,
}) => {
  const id = await api.createNegotiation('DRAFT', 'researcher2')
  const submits = []
  page.on('request', (r) => r.url().endsWith('/lifecycle/SUBMIT') && submits.push(r))

  await page.goto(`/edit/requests/${id}`)
  const submit = page.getByRole('button', { name: /Submit/ })
  // Race through the form wizard to its last step.
  while (!(await submit.isVisible())) await page.getByRole('button', { name: /Next/ }).click()
  await submit.click()
  await page.locator('#feedbackModal').getByRole('button', { name: 'Confirm' }).dblclick()

  await expect(page).toHaveURL(new RegExp(`/negotiations/${id}/ROLE_RESEARCHER`))
  await expect
    .poll(() => api.status(id), { message: `the UI sent ${submits.length} SUBMIT request(s)` })
    .toBe('SUBMITTED')
})

test('saving a draft in a second, older tab keeps the changes saved in the first tab', async ({
  browser,
  researcher2Page: firstTab,
  api,
}) => {
  // The same draft is open in two tabs (or by the author and a collaborator).
  const id = await api.createNegotiation('DRAFT', 'researcher2')
  const secondTab = await (
    await browser.newContext({ storageState: authFile('researcher2') })
  ).newPage()
  for (const tab of [firstTab, secondTab]) {
    await tab.goto(`/edit/requests/${id}?step=1`)
    await expect(tab.locator('input[type=text]:visible').first()).toBeVisible()
  }
  const title = `Renamed in the first tab ${Date.now()}`

  await firstTab.locator('input[type=text]:visible').first().fill(title)
  await firstTab.getByRole('button', { name: 'Save Draft' }).click()
  await expect.poll(async () => (await api.payload(id)).project.title).toBe(title)

  await secondTab.locator('textarea:visible').first().fill('Added in the second tab')
  await secondTab.getByRole('button', { name: 'Save Draft' }).click()
  await expect(secondTab.getByText('Negotiation saved correctly as draft')).toBeVisible()

  expect((await api.payload(id)).project.title, "the first tab's change was overwritten").toBe(
    title,
  )
  await secondTab.context().close()
})
