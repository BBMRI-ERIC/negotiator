package eu.bbmri_eric.negotiator.webhook.event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for newly added resources in a negotiation.
 *
 * @param negotiationId identifier of the affected negotiation
 */
@WebhookEventDoc(
    summary = "Resource was added to Negotiation",
    description = "Sent when one or more resources are added to an existing negotiation.")
@Schema(description = "Webhook data payload for newly added negotiation resources.")
record NegotiationResourceUpdatedWebhookEvent(
    @Schema(description = "Identifier of the affected negotiation", example = "negotiation-4")
        String negotiationId) {}
