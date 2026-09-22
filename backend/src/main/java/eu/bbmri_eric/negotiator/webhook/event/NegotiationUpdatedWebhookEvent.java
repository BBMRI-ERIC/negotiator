package eu.bbmri_eric.negotiator.webhook.event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for an updated negotiation.
 *
 * @param negotiationId identifier of the affected negotiation
 */
@WebhookEventDoc(
    summary = "Negotiation updated",
    description = "Sent when a negotiation was updated.")
@Schema(description = "Webhook data payload for an updated negotiation.")
record NegotiationUpdatedWebhookEvent(
    @Schema(
            description = "Identifier of the affected negotiation",
            example = "29f1819b-a1d4-48b6-a284-2f9e269bbd05")
        String negotiationId) {}
