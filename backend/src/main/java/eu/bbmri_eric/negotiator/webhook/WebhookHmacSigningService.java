package eu.bbmri_eric.negotiator.webhook;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
class WebhookHmacSigningService {

  private static final String SECRET_PREFIX = "whsec_";
  private static final String SIGNATURE_PREFIX = "v1,";
  private static final String HMAC_SHA_256 = "HmacSHA256";

  private final WebhookSecretService webhookSecretService;

  WebhookHmacSigningService(WebhookSecretService webhookSecretService) {
    this.webhookSecretService = webhookSecretService;
  }

  Optional<String> createSignature(
      String webhookId, long webhookTimestamp, String payload, String secretId) {
    if (secretId == null) {
      return Optional.empty();
    }

    String plainTextSecret = webhookSecretService.decryptSecret(secretId).plainText();
    byte[] secretBytes = decodeSecretBytes(plainTextSecret);
    String signedContent = "%s.%d.%s".formatted(webhookId, webhookTimestamp, payload);

    try {
      Mac mac = Mac.getInstance(HMAC_SHA_256);
      mac.init(new SecretKeySpec(secretBytes, HMAC_SHA_256));
      byte[] digest = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
      String encodedDigest = Base64.getEncoder().encodeToString(digest);
      return Optional.of(SIGNATURE_PREFIX + encodedDigest);
    } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
      throw new IllegalStateException("Could not create webhook signature", ex);
    }
  }

  private byte[] decodeSecretBytes(String plainTextSecret) {
    if (!StringUtils.startsWith(plainTextSecret, SECRET_PREFIX)) {
      throw new IllegalArgumentException("Secret must start with " + SECRET_PREFIX);
    }

    String encodedSecret = StringUtils.substringAfter(plainTextSecret, SECRET_PREFIX);
    if (StringUtils.isBlank(encodedSecret)) {
      throw new IllegalArgumentException("Secret must include base64 key material");
    }

    try {
      return Base64.getDecoder().decode(encodedSecret);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Secret key material must be valid base64", ex);
    }
  }
}
