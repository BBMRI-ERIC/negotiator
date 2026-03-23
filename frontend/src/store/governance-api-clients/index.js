import { NegotiatorClient } from './negotiatorClient'
import { PerunClient } from './perunClient'

export class GovernanceServiceFactory {
  static getClient() {
    switch (import.meta.env.VITE_GOVERNANCE_CLIENT) {
      case 'NEGOTIATOR':
        return new NegotiatorClient()
      case 'PERUN':
        return new PerunClient()
      default:
        throw new Error(`Unknown provider type: ${import.meta.env.VITE_GOVERNANCE_CLIENT}`)
    }
  }
}
