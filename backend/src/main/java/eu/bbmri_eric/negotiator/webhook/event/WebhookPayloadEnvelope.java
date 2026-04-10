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

  /** Creates a custom outbound payload envelope. */
  public static <T> WebhookPayloadEnvelope<T> custom(Instant occurredAt, T data) {
    return new WebhookPayloadEnvelope<>(WebhookEventType.CUSTOM, occurredAt, data);
  }
}
