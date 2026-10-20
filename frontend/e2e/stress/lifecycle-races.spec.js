import { test, expect } from '../fixtures.js'

// Lifecycle transitions hit by impatient users, retries and colleagues acting at the same time.
// The invariant is always the same: what the server answers must match what it stored.

test('researchers submitting different negotiations at the same moment are all submitted', async ({
  api,
}) => {
  // Eight researchers clicking "Submit" in the same second, e.g. right before a call deadline.
  const ids = await Promise.all(Array.from({ length: 8 }, () => api.createNegotiation('DRAFT')))

  const responses = await Promise.all(ids.map((id) => api.sendEvent(api.researcher, id, 'SUBMIT')))

  expect(responses.map((r) => r.status())).toEqual(ids.map(() => 200))
  await expect
    .poll(async () =>
      (await Promise.all(ids.map((id) => api.status(id)))).filter((s) => s === 'DRAFT'),
    )
    .toEqual([])
})

// The races below do not hit on every attempt, so each test plays several rounds.
const ROUNDS = 5

test('a retried submit request never leaves the negotiation in draft', async ({ api }) => {
  // Two identical requests within milliseconds: a double-click, or a client retrying on timeout.
  for (let round = 1; round <= ROUNDS; round++) {
    const id = await api.createNegotiation('DRAFT')

    const responses = await Promise.all(
      [1, 2].map(() => api.sendEvent(api.researcher, id, 'SUBMIT')),
    )

    expect(responses.some((r) => r.ok())).toBeTruthy()
    await expect.poll(() => api.status(id), { message: `round ${round}` }).toBe('SUBMITTED')
  }
})

test('two admins approving and declining at the same moment produce one decision', async ({
  adminPage,
  secondAdminPage,
  api,
}) => {
  for (let round = 1; round <= ROUNDS; round++) {
    await test.step(`round ${round}`, () => decideConcurrently(adminPage, secondAdminPage, api))
  }
})

async function decideConcurrently(adminPage, secondAdminPage, api) {
  const id = await api.createNegotiation('SUBMITTED')
  const decide = async (page, action) => {
    await page.goto(`/negotiations/${id}/ROLE_ADMIN`)
    await page.getByRole('button', { name: action }).click()
    return page.locator('.modal.show').getByRole('button', { name: 'Confirm' })
  }
  const [approve, decline] = await Promise.all([
    decide(adminPage, 'Approve'),
    decide(secondAdminPage, 'Decline'),
  ])
  const answers = []
  for (const page of [adminPage, secondAdminPage]) {
    page.on(
      'response',
      (r) => r.url().includes(`/negotiations/${id}/lifecycle/`) && answers.push(r),
    )
  }

  await Promise.all([approve.click(), decline.click()])

  await expect.poll(() => answers.length).toBe(2)
  const accepted = answers.filter((r) => r.ok()).map((r) => r.url().split('/').pop())
  expect(accepted, 'exactly one decision may be acknowledged').toHaveLength(1)
  const expected = accepted[0] === 'APPROVE' ? 'IN_PROGRESS' : 'DECLINED'
  await expect.poll(() => api.status(id)).toBe(expected)
}
