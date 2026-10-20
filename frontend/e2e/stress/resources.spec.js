import { test, expect } from '../fixtures.js'

// Representatives answering for their resources, in bulk and in parallel.

test('a representative updating several negotiations at once updates all of them', async ({
  api,
}) => {
  // A biobank working through its inbox with a script or several open tabs.
  const ids = []
  for (let i = 0; i < 6; i++) ids.push(await api.createNegotiation())

  const responses = await Promise.all(
    ids.map((id) => api.sendResourceEvent(id, 'MARK_AS_CHECKING_AVAILABILITY')),
  )

  expect(responses.map((r) => r.status())).toEqual(ids.map(() => 200))
  await expect
    .poll(async () => Promise.all(ids.map((id) => api.resourceState(id))))
    .toEqual(ids.map(() => 'CHECKING_AVAILABILITY'))
})
