// Test users from oidc-server-config/users.yaml.
export const USERS = {
  researcher: { username: 'Researcher', password: 'researcher' },
  // Owns no seed data; used by tests that depend on the author's list of drafts.
  researcher2: { username: 'Researcher2', password: 'researcher2' },
  biobanker: { username: 'Biobanker', password: 'biobanker' },
  admin: { username: 'admin', password: 'admin' },
}

export const authFile = (role) => `e2e/.auth/${role}.json`
