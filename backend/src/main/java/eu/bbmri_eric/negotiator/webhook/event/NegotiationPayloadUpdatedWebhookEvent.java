package eu.bbmri_eric.negotiator.webhook.event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for an updated negotiation payload.
 *
 * @param negotiationId identifier of the affected negotiation
 */
@WebhookEventDoc(
    summary = "Negotiation payload updated",
    description = "Sent when the payload of a negotiation was edited.")
@Schema(description = "Webhook data payload for an updated negotiation payload.")
record NegotiationPayloadUpdatedWebhookEvent(
    @Schema(description = "Identifier of the affected negotiation", example = "negotiation-1")
        String negotiationId) {}
