package eu.bbmri_eric.negotiator.webhook;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WebhookHeaders {
  public static final String SIGNATURE = "Webhook-Signature";
  public static final String TIMESTAMP = "Webhook-Timestamp";
  public static final String WEBHOOK_ID = "Webhook-Id";
}
