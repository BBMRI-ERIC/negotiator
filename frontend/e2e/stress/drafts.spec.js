import { test, expect } from '../fixtures.js'

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
