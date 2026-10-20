// Test users from oidc-server-config/users.yaml.
export const USERS = {
  researcher: { username: 'Researcher', password: 'researcher' },
  biobanker: { username: 'Biobanker', password: 'biobanker' },
  admin: { username: 'admin', password: 'admin' },
}

export const authFile = (role) => `e2e/.auth/${role}.json`
