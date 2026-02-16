package eu.bbmri_eric.negotiator.webhook;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WebhookHeaders {
  public static final String EVENT_TYPE = "X-Webhook-Event-Type";
}
