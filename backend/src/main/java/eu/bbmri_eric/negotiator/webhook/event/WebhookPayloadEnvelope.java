package eu.bbmri_eric.negotiator.webhook.event;

import java.time.Instant;

/**
 * Outbound webhook payload structure aligned with standard webhook recommendations.
 *
 * @param type semantic webhook event name exposed to consumers
 * @param timestamp UTC timestamp describing when the source event happened
 * @param data event-specific payload DTO
 */
public record WebhookPayloadEnvelope<T>(String type, Instant timestamp, T data) {

  /** Creates an outbound payload envelope from a semantic webhook event type. */
  public static <T> WebhookPayloadEnvelope<T> from(
      WebhookEventType eventType, Instant occurredAt, T data) {
    return new WebhookPayloadEnvelope<>(eventType.value(), occurredAt, data);
  }

  /** Creates a custom outbound payload envelope. */
  public static <T> WebhookPayloadEnvelope<T> custom(Instant occurredAt, T data) {
    return new WebhookPayloadEnvelope<>(WebhookEventType.CUSTOM.value(), occurredAt, data);
  }

  /** Converts the payload event type value into its enum representation. */
  public WebhookEventType eventType() {
    return WebhookEventType.fromValue(type);
  }
}
