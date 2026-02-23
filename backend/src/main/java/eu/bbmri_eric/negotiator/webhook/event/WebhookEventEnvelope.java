package eu.bbmri_eric.negotiator.webhook.event;

import java.time.Instant;

/**
 * Stable webhook payload envelope.
 *
 * @param eventType semantic webhook event name exposed to external consumers
 * @param occurredAt UTC timestamp describing when the source application event happened
 * @param data event-specific payload DTO
 */
public record WebhookEventEnvelope<T>(String eventType, Instant occurredAt, T data) {}
