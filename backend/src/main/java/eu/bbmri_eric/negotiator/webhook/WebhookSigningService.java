package eu.bbmri_eric.negotiator.webhook;

import java.util.Optional;

/** Service for generating signatures for outgoing webhook payloads. */
public interface WebhookSigningService {

  /**
   * Creates a signature for a webhook message.
   *
   * <p>The signed input is composed as {@code {webhookMessageId}.{webhookTimestamp}.{payload}}.
   *
   * @param webhookMessageId unique message id sent in webhook headers
   * @param webhookTimestamp message timestamp as Unix epoch seconds
   * @param payload JSON payload body to sign
   * @param secretId persisted secret identifier used for signing
   * @return generated signature, or an empty optional when no signing secret is configured
   */
  Optional<WebhookSignature> createSignature(
      String webhookMessageId, long webhookTimestamp, String payload, String secretId);
}
