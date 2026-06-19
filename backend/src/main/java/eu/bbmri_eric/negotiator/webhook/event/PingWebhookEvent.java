package eu.bbmri_eric.negotiator.webhook.event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for a ping delivery.
 *
 * @param webhookId identifier of the target webhook
 */
@WebhookEventDoc(
    summary = "Test delivery",
    description = "Sent when a webhook ping is manually triggered from the API.")
@Schema(description = "Webhook data payload for a ping delivery.")
public record PingWebhookEvent(
    @Schema(description = "Identifier of the target webhook", example = "1") Long webhookId) {}
