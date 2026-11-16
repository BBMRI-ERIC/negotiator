import governanceSettings from '@/config/governance'
import { NegotiatorClient } from '@/utils/governance/negotiatorClient'
import { PerunClient } from '@/utils/governance/perunClient'

// The client instance is cached at module level because it holds mutable state
// (e.g. PerunClient caches the Perun groups tree in its PerunGroupsManager).
// A single shared instance must therefore be returned to all consumers.
let client = null

export function useGovernanceClient() {
  if (client == null) {
    switch (governanceSettings.client) {
      case 'NEGOTIATOR':
        client = NegotiatorClient()
        break
      case 'PERUN':
        client = PerunClient()
        break
      default:
        throw new Error(`Unknown governance provider: ${governanceSettings.client}`)
    }
  }
  return client
}

export function resetGovernanceClient() {
  client = null
}
