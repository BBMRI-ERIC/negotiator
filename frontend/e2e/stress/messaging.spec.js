import { test, expect } from '../fixtures.js'

const open = (page, id, role) => page.goto(`/negotiations/${id}/${role}`)

async function sendMessage(page, text) {
  await page.locator('textarea.mb-3').fill(text)
  await page.locator('#recipient').selectOption('public')
  await page.locator('#send').click()
  // Wait until the form accepted the message before typing the next one, as fast as possible.
  await expect(page.locator('textarea.mb-3')).toHaveValue('')
}

test('researcher and representative fire messages at each other simultaneously', async ({
  researcherPage,
  biobankerPage,
  api,
}) => {
  const id = await api.createNegotiation()
  const run = Date.now()
  const burst = (who) => Array.from({ length: 10 }, (_, i) => `${who} ${run} #${i}`)
  const researcherMessages = burst('researcher')
  const biobankerMessages = burst('biobanker')
  await Promise.all([
    open(researcherPage, id, 'ROLE_RESEARCHER'),
    open(biobankerPage, id, 'ROLE_REPRESENTATIVE'),
  ])

  await Promise.all([
    (async () => {
      for (const text of researcherMessages) await sendMessage(researcherPage, text)
    })(),
    (async () => {
      for (const text of biobankerMessages) await sendMessage(biobankerPage, text)
    })(),
  ])

  const all = [...researcherMessages, ...biobankerMessages]
  const stored = (await api.posts(id)).map((p) => p.text).filter((t) => t.includes(`${run}`))
  expect(stored.sort()).toEqual([...all].sort())
  for (const page of [researcherPage, biobankerPage]) {
    await page.reload()
    for (const text of all) await expect(page.getByText(text, { exact: true })).toHaveCount(1)
  }
})

test('a double-clicked send button posts the message once', async ({ researcherPage, api }) => {
  const id = await api.createNegotiation()
  const text = `double click ${Date.now()}`
  await open(researcherPage, id, 'ROLE_RESEARCHER')

  await researcherPage.locator('textarea.mb-3').fill(text)
  await researcherPage.locator('#recipient').selectOption('public')
  await researcherPage.locator('#send').dblclick()

  await expect(researcherPage.getByText(text, { exact: true })).toHaveCount(1)
  await expect.poll(async () => (await api.posts(id)).filter((p) => p.text === text).length).toBe(1)
})

test('a message written on a stale page is not silently accepted after the negotiation is abandoned', async ({
  biobankerPage,
  api,
}) => {
  const id = await api.createNegotiation()
  const text = `too late ${Date.now()}`
  await open(biobankerPage, id, 'ROLE_REPRESENTATIVE')
  await biobankerPage.locator('textarea.mb-3').fill(text)
  await biobankerPage.locator('#recipient').selectOption('public')

  // Meanwhile, in another browser, the researcher abandons the negotiation.
  expect((await api.sendEvent(api.researcher, id, 'ABANDON')).ok()).toBeTruthy()
  await expect.poll(() => api.status(id)).toBe('ABANDONED')

  const answer = biobankerPage.waitForResponse((r) => r.url().endsWith(`/negotiations/${id}/posts`))
  await biobankerPage.locator('#send').click()

  expect((await answer).ok(), 'posting to an abandoned negotiation must be rejected').toBeFalsy()
  expect((await api.posts(id)).map((p) => p.text)).not.toContain(text)
  await expect(biobankerPage.getByText('Error sending message')).toBeVisible()
  // The user must not lose what they wrote just because the send failed.
  await expect(biobankerPage.locator('textarea.mb-3')).toHaveValue(text)
})
