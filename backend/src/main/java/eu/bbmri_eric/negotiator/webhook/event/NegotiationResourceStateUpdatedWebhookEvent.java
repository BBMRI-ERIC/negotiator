package eu.bbmri_eric.negotiator.webhook.event;

import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;

/**
 * Webhook data payload for a resource state transition in a negotiation.
 *
 * @param negotiationId identifier of the parent negotiation
 * @param resourceId identifier of the affected resource
 * @param fromState previous resource state
 * @param toState new resource state
 * @param event resource-state-machine event that triggered the transition
 */
record NegotiationResourceStateUpdatedWebhookEvent(
    String negotiationId,
    String resourceId,
    NegotiationResourceState fromState,
    NegotiationResourceState toState,
    NegotiationResourceEvent event) {}
