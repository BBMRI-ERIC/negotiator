package eu.bbmri_eric.negotiator.webhook.event;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for a newly created negotiation.
 *
 * @param negotiationId identifier of the created negotiation
 * @param currentState state of the created negotiation
 */
@WebhookEventDoc(
    summary = "Negotiation added",
    description = "Sent when a new negotiation is created in the Negotiator.")
@Schema(description = "Webhook data payload for a newly created negotiation.")
record NegotiationAddedWebhookEvent(
    @Schema(description = "Identifier of the created negotiation", example = "negotiation-3")
        String negotiationId,
    @Schema(description = "Current negotiation state", example = "SUBMITTED")
        NegotiationState currentState) {}
