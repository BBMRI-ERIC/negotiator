import fs from 'node:fs'
import { test, expect } from '../fixtures.js'
import { RESOURCE } from '../api.js'

// Walks typical user journeys through the real UI and records every API call they cause, so the
// load tests in /load replay the traffic the frontend really produces. Run with
// `yarn e2e:record`; the result is written to ../load/recorded-traffic.json.

const OUTPUT = '../load/recorded-traffic.json'
const journeys = {}

const ID = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|(?<=\/)\d+(?=\/|$)/g

function record(page, journey) {
  const calls = (journeys[journey] = [])
  let step = 'start'
  page.on('requestfinished', async (request) => {
    const url = new URL(request.url())
    if (!url.pathname.startsWith('/api/')) return
    const timing = request.timing()
    calls.push({
      step,
      method: request.method(),
      path: decodeURIComponent(url.pathname)
        .replace(ID, '{id}')
        .replace(/bbmri-eric:[^/]+/, '{resourceId}'),
      query: url.search.replace(ID, '{id}'),
      status: (await request.response())?.status(),
      ms: Math.round(timing.responseEnd),
    })
  })
  return (name) => (step = name)
}

test.afterAll(() => {
  fs.writeFileSync(OUTPUT, JSON.stringify(journeys, null, 2) + '\n')
})

test('researcher checks their negotiations and answers a message', async ({
  researcherPage: page,
  api,
}) => {
  const id = await api.createNegotiation()
  const step = record(page, 'researcher')

  step('open list')
  await page.goto('/researcher')
  await expect(page.locator('.primary-table-row').first()).toBeVisible()
  step('open negotiation')
  await page.goto(`/negotiations/${id}/ROLE_RESEARCHER`)
  await expect(page.locator('#send')).toBeVisible()
  step('send message')
  await page.locator('textarea.mb-3').fill('Load test message')
  await page.locator('#recipient').selectOption('public')
  await page.locator('#send').click()
  await expect(page.getByText('Load test message')).toBeVisible()
  await page.waitForLoadState('networkidle')
})

test('representative works through the biobank inbox', async ({ biobankerPage: page, api }) => {
  const id = await api.createNegotiation()
  const step = record(page, 'representative')

  step('open list')
  await page.goto('/biobanker')
  await expect(page.locator('.primary-table-row').first()).toBeVisible()
  step('open negotiation')
  await page.goto(`/negotiations/${id}/ROLE_REPRESENTATIVE`)
  await expect(page.locator('#send')).toBeVisible()
  step('send message')
  await page.locator('textarea.mb-3').fill('Load test answer')
  await page.locator('#recipient').selectOption('public')
  await page.locator('#send').click()
  await expect(page.getByText('Load test answer')).toBeVisible()
  await page.waitForLoadState('networkidle')
})

test('admin reviews and approves a submitted negotiation', async ({ adminPage: page, api }) => {
  const id = await api.createNegotiation('SUBMITTED')
  const step = record(page, 'admin')

  step('open list')
  await page.goto('/admin')
  await expect(page.locator('.primary-table-row').first()).toBeVisible()
  step('open negotiation')
  await page.goto(`/negotiations/${id}/ROLE_ADMIN`)
  await expect(page.getByRole('button', { name: 'Approve' })).toBeVisible()
  step('approve')
  await page.getByRole('button', { name: 'Approve' }).click()
  await page.locator('.modal.show').getByRole('button', { name: 'Confirm' }).click()
  await expect.poll(() => api.status(id)).toBe('IN_PROGRESS')
  await page.waitForLoadState('networkidle')
})

test('researcher arrives from the Directory and submits a new request', async ({
  researcher2Page: page,
  api,
}) => {
  await api.deleteDrafts('researcher2')
  // The Directory hands the request over; the browser is then redirected to the Negotiator.
  const request = await (
    await api.clients.researcher2.post('requests', {
      data: { url: 'https://bbmritestnn.gcc.rug.nl', humanReadable: 'load', resources: [RESOURCE] },
    })
  ).json()
  const step = record(page, 'new request')

  step('open handover link')
  await page.goto(`/requests/${request.id}`)
  await expect(page).toHaveURL(/\/edit\/requests\//)
  step('walk through form')
  const submit = page.getByRole('button', { name: /Submit/ })
  while (!(await submit.isVisible())) {
    for (const field of await page.locator('.form-control:visible').all()) {
      const type = (await field.getAttribute('type'))?.toLowerCase()
      if (type === 'file') continue
      await field.fill(type === 'number' ? '10' : 'Load test input')
    }
    for (const option of await page.locator('input[type=radio]:visible').all()) await option.check()
    await page.getByRole('button', { name: /Next/ }).click()
  }
  step('submit')
  await submit.click()
  await page.locator('#feedbackModal').getByRole('button', { name: 'Confirm' }).click()
  await expect(page).toHaveURL(/ROLE_RESEARCHER/)
  await page.waitForLoadState('networkidle')
  await api.deleteDrafts('researcher2')
})
