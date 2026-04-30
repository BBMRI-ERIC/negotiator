package eu.bbmri_eric.negotiator.webhook;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Immutable representation of a webhook signature header value.
 *
 * <p>The serialized format is {@code {version},{digest}}.
 *
 * @param version signature version identifier
 * @param digest base64-encoded signature digest
 */
public record WebhookSignature(
    @NonNull WebhookSignatureIdentifier version, @NonNull String digest) {
  private static final char SEPARATOR = ',';

  @Override
  public String toString() {
    return version.toString() + SEPARATOR + digest;
  }

  /**
   * Creates an HMAC-SHA256 webhook signature
   *
   * @param digest base64-encoded HMAC-SHA256 digest
   * @return versioned webhook signature
   */
  public static WebhookSignature hmacSha256(String digest) {
    return new WebhookSignature(WebhookSignatureIdentifier.HMAC_SHA_256, digest);
  }
}

/** Supported version identifiers for webhook signatures. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
enum WebhookSignatureIdentifier {
  /** HMAC-SHA256 signature identifier. */
  HMAC_SHA_256("v1");

  private final String code;

  @Override
  public String toString() {
    return code;
  }
}
