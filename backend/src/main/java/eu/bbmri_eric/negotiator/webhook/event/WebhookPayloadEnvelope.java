package eu.bbmri_eric.negotiator.webhook.event;

import java.time.Instant;

/**
 * Outbound webhook payload structure aligned with standard webhook recommendations.
 *
 * @param type semantic webhook event name exposed to consumers
 * @param timestamp UTC timestamp describing when the source event happened
 * @param data event-specific payload DTO
 */
public record WebhookPayloadEnvelope<T>(WebhookEventType type, Instant timestamp, T data) {

  /** Creates a ping outbound payload envelope. */
  public static WebhookPayloadEnvelope<?> ping(Long webhookId, Instant occurredAt) {
    return new WebhookPayloadEnvelope<>(
        WebhookEventType.PING, occurredAt, new PingWebhookEvent(webhookId));
  }
}
