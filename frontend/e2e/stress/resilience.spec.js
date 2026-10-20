import { test, expect } from '../fixtures.js'

// Bad networks, slow servers and hostile input.

const open = (page, id) => page.goto(`/negotiations/${id}/ROLE_RESEARCHER`)

async function writeMessage(page, text) {
  await page.locator('textarea.mb-3').fill(text)
  await page.locator('#recipient').selectOption('public')
}

test('a message survives a dropped connection and is posted once on retry', async ({
  researcherPage: page,
  api,
}) => {
  const id = await api.createNegotiation()
  const text = `train tunnel ${Date.now()}`
  await open(page, id)
  // The first attempt dies on the way to the server, like a train entering a tunnel.
  let dropped = false
  await page.route(`**/negotiations/${id}/posts`, (route) => {
    if (dropped || route.request().method() !== 'POST') return route.fallback()
    dropped = true
    return route.abort('internetdisconnected')
  })

  await writeMessage(page, text)
  await page.locator('#send').click()
  await expect(page.getByText('Error sending message')).toBeVisible()
  await expect(page.locator('textarea.mb-3'), 'the text must survive the failure').toHaveValue(text)

  await page.locator('#recipient').selectOption('public')
  await page.locator('#send').click()
  await expect(page.getByText(text, { exact: true })).toHaveCount(1)
  expect((await api.posts(id)).filter((p) => p.text === text)).toHaveLength(1)
})

test('a slow, outdated list response does not overwrite the current filter', async ({
  researcherPage: page,
  api,
}) => {
  await api.createNegotiation('SUBMITTED')
  await page.goto('/researcher')
  await expect(page.locator('.primary-table-row').first()).toBeVisible()
  // The server is slow to answer the first filter the user picks.
  await page.route(
    (url) => url.pathname.endsWith('/negotiations') && url.search.includes('SUBMITTED'),
    async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 2000))
      await route.continue()
    },
  )

  await page.getByRole('button', { name: 'Filter by status' }).click()
  const filter = (label) => page.locator('.form-check', { hasText: label }).locator('input')
  await filter('Under review').check()
  // Changes their mind before the answer arrives.
  await filter('Under review').uncheck()
  await filter('Concluded').check()
  await page.waitForTimeout(3000)

  const rows = page.locator('.primary-table-row')
  await expect(rows.first()).toBeVisible()
  await expect(rows.filter({ hasText: 'UNDER REVIEW' })).toHaveCount(0)
})

test('markup, emoji, right-to-left text and newlines are shown exactly as typed', async ({
  researcherPage: page,
  biobankerPage,
  api,
}) => {
  const id = await api.createNegotiation()
  const marker = Date.now()
  const lines = [
    `<img src=x onerror="window.__xss=${marker}"> & "quotes" 'single' ${marker}`,
    '🧬 samples für Ärzte – שלום – 你好',
  ]
  await open(page, id)

  await writeMessage(page, lines.join('\n'))
  await page.locator('#send').click()

  await biobankerPage.goto(`/negotiations/${id}/ROLE_REPRESENTATIVE`)
  for (const viewer of [page, biobankerPage]) {
    const post = viewer.locator('.card', { hasText: `${marker}` }).last()
    for (const line of lines) await expect(post).toContainText(line)
    expect(await viewer.evaluate(() => window.__xss)).toBeUndefined()
  }
})

test('a very long pasted message is either posted intact or rejected visibly', async ({
  researcherPage: page,
  api,
}) => {
  const id = await api.createNegotiation()
  const text = `long ${Date.now()} ` + 'Lorem ipsum dolor sit amet. '.repeat(4000)
  await open(page, id)

  await writeMessage(page, text)
  const answer = page.waitForResponse((r) => r.url().endsWith(`/negotiations/${id}/posts`))
  await page.locator('#send').click()

  if ((await answer).ok()) {
    expect((await api.posts(id)).map((p) => p.text)).toContain(text)
  } else {
    await expect(page.getByText('Error sending message')).toBeVisible()
    await expect(page.locator('textarea.mb-3')).toHaveValue(text)
  }
})

test('several attachments uploaded at the same moment are all kept', async ({ api }) => {
  // Dragging five files onto the page at once.
  const id = await api.createNegotiation()
  const names = Array.from({ length: 5 }, (_, i) => `file-${i}.txt`)

  const responses = await Promise.all(
    names.map((name) => api.upload(api.researcher, id, name, `content of ${name}`)),
  )

  expect(responses.map((r) => r.status())).toEqual(names.map(() => 201))
  const stored = (await api.attachments(id)).map((a) => a.name)
  expect(stored.sort()).toEqual(names)
})
