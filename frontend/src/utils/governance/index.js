import { NegotiatorClient } from './negotiatorClient'
import { PerunClient } from './perunClient'

export function getGovernanceClient() {
  if (getGovernanceClient.client == null) {
    switch (import.meta.env.VITE_GOVERNANCE_CLIENT) {
      case 'NEGOTIATOR':
        getGovernanceClient.client = NegotiatorClient()
        break
      case 'PERUN':
        getGovernanceClient.client = PerunClient()
        break
      default:
        throw new Error(`Unknown provider type: ${import.meta.env.VITE_GOVERNANCE_CLIENT}`)
    }
  }
  return getGovernanceClient.client
}
