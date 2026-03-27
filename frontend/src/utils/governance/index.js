import governanceSettings from '@/config/governanceSettings'
import { NegotiatorClient } from './negotiatorClient'
import { PerunClient } from './perunClient'

export function getGovernanceClient() {
  if (getGovernanceClient.client == null) {
    switch (governanceSettings.client) {
      case 'NEGOTIATOR':
        getGovernanceClient.client = NegotiatorClient()
        break
      case 'PERUN':
        getGovernanceClient.client = PerunClient()
        break
      default:
        throw new Error(`Unknown provider type: ${governanceSettings.client}`)
    }
  }
  return getGovernanceClient.client
}
