import { test, expect } from '../fixtures.js'

// People who should not (or no longer) see a negotiation, acting faster than permissions refresh.

test('an uninvolved user with a forwarded link sees nothing of the negotiation', async ({
  researcher2Page: page,
  api,
}) => {
  const id = await api.createNegotiation()
  const secret = `confidential ${Date.now()}`
  await api.researcher.post(`negotiations/${id}/posts`, { data: { text: secret, type: 'PUBLIC' } })
  const attachment = await (await api.upload(api.researcher, id, 'protocol.txt', secret)).json()
  const outsider = api.clients.researcher2

  for (const path of [
    `negotiations/${id}`,
    `negotiations/${id}/posts`,
    `negotiations/${id}/timeline`,
    `negotiations/${id}/resources`,
    `negotiations/${id}/attachments/${attachment.id}`,
    `attachments/${attachment.id}`,
  ]) {
    expect((await outsider.get(path)).status(), path).toBe(403)
  }
  const list = await outsider.get(`negotiations/${id}/attachments`)
  expect(list.status() === 403 || (await list.json()).length === 0).toBeTruthy()

  await page.goto(`/negotiations/${id}/ROLE_RESEARCHER`)
  await expect(page.getByText(secret)).toHaveCount(0)
})

test('a collaborator removed while their page is open can no longer post', async ({
  researcher2Page: page,
  api,
}) => {
  const id = await api.createNegotiation()
  const collaborator = await api.userId('researcher2')
  expect((await api.researcher.post(`negotiations/${id}/collaborators/${collaborator}`)).ok()).toBe(
    true,
  )
  await page.goto(`/negotiations/${id}/ROLE_RESEARCHER`)
  const text = `after removal ${Date.now()}`
  await page.locator('textarea.mb-3').fill(text)
  await page.locator('#recipient').selectOption('public')

  // The author removes the collaborator while the collaborator is still typing.
  expect(
    (await api.researcher.delete(`negotiations/${id}/collaborators/${collaborator}`)).ok(),
  ).toBe(true)
  await page.locator('#send').click()

  await expect(page.getByText('Error sending message')).toBeVisible()
  expect((await api.posts(id)).map((p) => p.text)).not.toContain(text)
})
