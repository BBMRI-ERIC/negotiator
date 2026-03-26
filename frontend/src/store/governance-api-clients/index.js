import { NegotiatorClient } from './negotiatorClient'
import { PerunClient } from './perunClient'

export class GovernanceServiceFactory {
  client = null

  static getClient() {
    if (this.client == null) {
      switch (import.meta.env.VITE_GOVERNANCE_CLIENT) {
        case 'NEGOTIATOR':
          this.client = new NegotiatorClient()
          break
        case 'PERUN':
          this.client = new PerunClient()
          break
        default:
          throw new Error(`Unknown provider type: ${import.meta.env.VITE_GOVERNANCE_CLIENT}`)
      }
    }
    return this.client
  }
}
