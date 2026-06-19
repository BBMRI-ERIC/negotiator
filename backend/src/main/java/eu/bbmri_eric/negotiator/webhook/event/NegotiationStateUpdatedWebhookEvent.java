package eu.bbmri_eric.negotiator.webhook.event;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for a negotiation state transition.
 *
 * @param negotiationId identifier of the affected negotiation
 * @param fromState source negotiation state before transition
 * @param toState target negotiation state after transition
 * @param event state-machine event that triggered the transition
 */
@WebhookEventDoc(
    summary = "Negotiation state changed",
    description = "Sent when the negotiation lifecycle state transitions to a new state.")
@Schema(description = "Webhook data payload for a negotiation state transition.")
record NegotiationStateUpdatedWebhookEvent(
    @Schema(description = "Identifier of the affected negotiation", example = "negotiation-2")
        String negotiationId,
    @Schema(description = "Source state before transition", example = "DRAFT")
        NegotiationState fromState,
    @Schema(description = "Target state after transition", example = "SUBMITTED")
        NegotiationState toState,
    @Schema(description = "State-machine event that triggered the transition", example = "SUBMIT")
        NegotiationEvent event) {}
