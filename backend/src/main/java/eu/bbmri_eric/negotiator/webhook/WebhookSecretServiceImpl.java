package eu.bbmri_eric.negotiator.webhook;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class WebhookSecretServiceImpl implements WebhookSecretService {

  private static final int SALT_LENGTH_BYTES = 16;
  private static final int MIN_SECRET_BYTES = 24;
  private static final int MAX_SECRET_BYTES = 64;
  private static final String SECRET_PREFIX = "whsec_";
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  private final WebhookSecretRepository webhookSecretRepository;
  private final WebhookTextEncryptorFactory webhookTextEncryptorFactory;
  private final SecureRandom secureRandom;

  public WebhookSecretServiceImpl(
      WebhookSecretRepository webhookSecretRepository,
      WebhookTextEncryptorFactory webhookTextEncryptorFactory) {
    this.webhookSecretRepository = webhookSecretRepository;
    this.webhookTextEncryptorFactory = webhookTextEncryptorFactory;
    this.secureRandom = new SecureRandom();
  }

  @Override
  public String validateSecret(String plainTextSecret) {
    if (StringUtils.isBlank(plainTextSecret)) {
      throw new IllegalArgumentException("Secret must not be blank");
    }

    if (!StringUtils.startsWith(plainTextSecret, SECRET_PREFIX)) {
      throw new IllegalArgumentException("Secret must start with " + SECRET_PREFIX);
    }

    String encodedSecret = StringUtils.substringAfter(plainTextSecret, SECRET_PREFIX);
    if (StringUtils.isBlank(encodedSecret)) {
      throw new IllegalArgumentException("Secret must include base64 key material");
    }

    byte[] decodedSecret = decodeSecret(encodedSecret);
    if (decodedSecret.length < MIN_SECRET_BYTES || decodedSecret.length > MAX_SECRET_BYTES) {
      throw new IllegalArgumentException(
          "Secret key material must decode to between %d and %d bytes"
              .formatted(MIN_SECRET_BYTES, MAX_SECRET_BYTES));
    }

    return plainTextSecret;
  }

  @Override
  @Transactional
  public DecryptedWebhookSecret createSecret(String plainTextSecret) {
    String validatedSecret = validateSecret(plainTextSecret);
    String salt = generateRandomSalt();
    String encrypted = webhookTextEncryptorFactory.create(salt).encrypt(validatedSecret);
    var secret = new WebhookSecret(encrypted, salt);
    WebhookSecret saved = webhookSecretRepository.save(secret);
    return new DecryptedWebhookSecret(saved.getId(), validatedSecret);
  }

  @Override
  @Transactional
  public void deleteSecret(String secretId) {
    if (!webhookSecretRepository.existsById(secretId)) {
      throw new EntityNotFoundException(secretId);
    }
    webhookSecretRepository.deleteById(secretId);
  }

  @Override
  @Transactional(readOnly = true)
  public DecryptedWebhookSecret decryptSecret(String secretId) {
    WebhookSecret secret =
        webhookSecretRepository
            .findById(secretId)
            .orElseThrow(() -> new EntityNotFoundException(secretId));
    String plainText =
        webhookTextEncryptorFactory.create(secret.getSalt()).decrypt(secret.getEncryptedSecret());
    return new DecryptedWebhookSecret(secret.getId(), plainText);
  }

  protected String generateRandomSalt() {
    byte[] bytes = new byte[SALT_LENGTH_BYTES];
    secureRandom.nextBytes(bytes);
    return HEX_FORMAT.formatHex(bytes);
  }

  private byte[] decodeSecret(String encodedSecret) {
    try {
      return Base64.getDecoder().decode(encodedSecret);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Secret key material must be valid base64", ex);
    }
  }
}
