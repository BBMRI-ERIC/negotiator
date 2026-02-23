package eu.bbmri_eric.negotiator.webhook.event;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;

/**
 * Webhook data payload for a negotiation state transition.
 *
 * @param negotiationId identifier of the affected negotiation
 * @param changedTo target negotiation state after transition
 * @param event state-machine event that triggered the transition
 * @param post optional post payload associated with the transition
 */
record NegotiationStateUpdatedWebhookEvent(
    String negotiationId, NegotiationState changedTo, NegotiationEvent event, String post) {}
