package eu.bbmri_eric.negotiator.webhook.event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for a newly created negotiation.
 *
 * <p>The State is a name carried as data: it is a JSON string on the wire, exactly as it was when
 * this record held an enum.
 *
 * @param negotiationId identifier of the created negotiation
 * @param currentState name of the state of the created negotiation
 */
@WebhookEventDoc(
    summary = "Negotiation added",
    description = "Sent when a new negotiation is created in the Negotiator.")
@Schema(description = "Webhook data payload for a newly created negotiation.")
record NegotiationAddedWebhookEvent(
    @Schema(description = "Identifier of the created negotiation", example = "negotiation-3")
        String negotiationId,
    @Schema(description = "Name of the current negotiation state", example = "SUBMITTED")
        String currentState) {}
