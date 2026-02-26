package eu.bbmri_eric.negotiator.webhook.event;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;

/**
 * Webhook data payload for a negotiation state transition.
 *
 * @param negotiationId identifier of the affected negotiation
 * @param fromState source negotiation state before transition
 * @param toState target negotiation state after transition
 * @param event state-machine event that triggered the transition
 */
record NegotiationStateUpdatedWebhookEvent(
    String negotiationId,
    NegotiationState fromState,
    NegotiationState toState,
    NegotiationEvent event) {}
