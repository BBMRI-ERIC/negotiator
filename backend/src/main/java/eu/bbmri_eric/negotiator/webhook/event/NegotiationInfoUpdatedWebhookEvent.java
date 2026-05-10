package eu.bbmri_eric.negotiator.webhook.event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for an updated information submission in a negotiation.
 *
 * @param negotiationId identifier of the affected negotiation
 */
@WebhookEventDoc(
    summary = "Negotiation information updated",
    description = "Sent when additional information for a negotiation was submitted or updated.")
@Schema(description = "Webhook data payload for updated negotiation information.")
record NegotiationInfoUpdatedWebhookEvent(
    @Schema(description = "Identifier of the affected negotiation", example = "negotiation-1")
        String negotiationId) {}
