package eu.bbmri_eric.negotiator.webhook;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

public record WebhookSignature(
    @NonNull WebhookSignatureIdentifier version, @NonNull String digest) {
  private static final char SEPARATOR = ',';

  @Override
  public String toString() {
    return version.toString() + SEPARATOR + digest;
  }

  public static WebhookSignature hmacSha256(String digest) {
    return new WebhookSignature(WebhookSignatureIdentifier.HMAC_SHA_256, digest);
  }
}

@AllArgsConstructor(access = AccessLevel.PRIVATE)
enum WebhookSignatureIdentifier {
  HMAC_SHA_256("v1");

  private final String code;

  @Override
  public String toString() {
    return code;
  }
}
