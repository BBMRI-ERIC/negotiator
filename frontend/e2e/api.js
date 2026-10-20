import fs from 'node:fs'
import { request, expect } from '@playwright/test'
import { authFile } from './users.js'

// Seeded collection represented by the Biobanker test user.
export const RESOURCE = {
  id: 'bbmri-eric:ID:SE_890:collection:dummy_collection',
  name: 'Dummy test collection',
  organization: { externalId: 'bbmri-eric:ID:SE_890', name: 'Biobank Väst' },
}

// A payload that satisfies the default access form.
const PAYLOAD = {
  project: {
    title: 'Playwright stress test',
    'disease-code': 'E11.9',
    objective: 'Exercise concurrent workflows',
    organization: 'Playwright',
    profit: 'No',
    acknowledgment: 'None',
    'Multichoice test': ['first_choice'],
    'Single test': 'first_choice',
    'Bool test': 'Yes',
  },
  request: {
    description: 'Created by an e2e test',
    collection: '',
    donors: '10',
    samples: 'blood',
    specifics: 'None',
  },
  'ethics-vote': { 'ethics-vote': 'Approved', 'ethics-vote-attachment': null },
}

// The OIDC user that pinia-oidc stored in localStorage during auth.setup.js.
function accessToken(role) {
  const { origins } = JSON.parse(fs.readFileSync(authFile(role), 'utf8'))
  const entry = origins
    .flatMap((origin) => origin.localStorage)
    .find(({ name }) => name.startsWith('oidc.user:'))
  return JSON.parse(entry.value).access_token
}

export async function apiAs(role, baseURL) {
  return request.newContext({
    baseURL: `${baseURL}/api/v3/`,
    extraHTTPHeaders: { Authorization: `Bearer ${accessToken(role)}` },
  })
}

async function ok(response) {
  expect(response.ok(), `${response.url()} -> ${response.status()}`).toBeTruthy()
  return response.json()
}

export class NegotiatorApi {
  constructor(clients) {
    this.clients = clients
    this.researcher = clients.researcher
    this.admin = clients.admin
    this.biobanker = clients.biobanker
  }

  static async create(baseURL, roles) {
    const entries = await Promise.all(roles.map(async (role) => [role, await apiAs(role, baseURL)]))
    return new NegotiatorApi(Object.fromEntries(entries))
  }

  // Creates a draft as `author`, then moves it as far as `until` ('DRAFT', 'SUBMITTED' or
  // 'IN_PROGRESS').
  async createNegotiation(until = 'IN_PROGRESS', author = 'researcher') {
    const client = this.clients[author]
    const req = await ok(
      await client.post('requests', {
        data: {
          url: 'https://bbmritestnn.gcc.rug.nl',
          humanReadable: 'e2e',
          resources: [RESOURCE],
        },
      }),
    )
    const { id } = await ok(
      await client.post('negotiations', {
        data: { request: req.id, payload: PAYLOAD, draft: true },
      }),
    )
    if (until === 'DRAFT') return id
    await this.transition(client, id, 'SUBMIT', 'SUBMITTED')
    if (until === 'SUBMITTED') return id
    await this.transition(this.admin, id, 'APPROVE', 'IN_PROGRESS')
    return id
  }

  sendEvent(client, id, event) {
    return client.put(`negotiations/${id}/lifecycle/${event}`, { data: {} })
  }

  // Setup only: lifecycle events sent concurrently from parallel tests can be dropped while the
  // API still answers 200 (see lifecycle-races.spec.js), so resend until the state has changed.
  async transition(client, id, event, expected) {
    await expect(async () => {
      if ((await this.status(id)) !== expected) await this.sendEvent(client, id, event)
      await expect.poll(() => this.status(id), { timeout: 1000 }).toBe(expected)
    }).toPass({ timeout: 15000 })
  }

  async status(id) {
    return (await ok(await this.admin.get(`negotiations/${id}`))).status
  }

  async exists(id) {
    return (await this.admin.get(`negotiations/${id}`)).ok()
  }

  async deleteDrafts(author) {
    const { id: userId } = await ok(await this.clients[author].get('userinfo'))
    const body = await ok(
      await this.clients[author].get(`users/${userId}/negotiations?role=author&status=DRAFT`),
    )
    for (const { id } of body._embedded?.negotiations ?? []) {
      await this.clients[author].delete(`negotiations/${id}`)
    }
  }

  async posts(id) {
    const body = await ok(await this.researcher.get(`negotiations/${id}/posts`))
    return body._embedded?.posts ?? []
  }

  async timeline(id) {
    const body = await ok(await this.researcher.get(`negotiations/${id}/timeline`))
    return body._embedded?.timelineEvents ?? []
  }

  async dispose() {
    await Promise.all(Object.values(this.clients).map((c) => c.dispose()))
  }
}
