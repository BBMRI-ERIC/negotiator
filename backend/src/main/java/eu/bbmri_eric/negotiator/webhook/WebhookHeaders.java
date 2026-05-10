package eu.bbmri_eric.negotiator.webhook;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WebhookHeaders {
  @WebhookHeaderDoc(
      description = "Optional webhook signature in the format v1,<base64-digest>",
      required = false,
      example = "v1,1+jttfc8wkgmg2OT+W+HYN8WwS7i4o+UWhIQuvPzHMg=")
  public static final String SIGNATURE = "Webhook-Signature";

  @WebhookHeaderDoc(
      description = "Unix epoch timestamp in seconds",
      required = true,
      example = "1713943330")
  public static final String TIMESTAMP = "Webhook-Timestamp";

  @WebhookHeaderDoc(
      description = "Unique webhook message identifier used for signature verification",
      required = true,
      example = "123e4567-e89b-12d3-a456-426614174000")
  public static final String WEBHOOK_ID = "Webhook-Id";
}
