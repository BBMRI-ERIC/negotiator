package eu.bbmri_eric.negotiator.webhook;

import java.util.Optional;

public interface WebhookSigningService {

  Optional<WebhookSignature> createSignature(
      String webhookMessageId, long webhookTimestamp, String payload, String secretId);
}
