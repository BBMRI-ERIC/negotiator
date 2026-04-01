package eu.bbmri_eric.negotiator.webhook;

/** Service for managing webhook secrets with encryption at rest. */
public interface WebhookSecretService {

  /**
   * Validates a webhook signing secret.
   *
   * <p>The secret must use the exact {@code whsec_} prefix, must contain valid base64-encoded key
   * material , and must decode to 24-64 bytes.
   *
   * @param plainTextSecret secret provided by user
   * @return validated secret
   */
  String validateSecret(String plainTextSecret);

  /**
   * Creates a new encrypted secret from a user-provided plaintext value.
   *
   * @param plainTextSecret plaintext secret provided by user
   * @return created secret containing id and plaintext
   */
  DecryptedWebhookSecret createSecret(String plainTextSecret);

  /**
   * Deletes a secret by its id.
   *
   * @param secretId the id of the secret to delete
   */
  void deleteSecret(String secretId);

  /**
   * Decrypts and returns the plaintext for a given secret id. Intended for internal use only (e.g.,
   * HMAC signing).
   *
   * @param secretId the id of the secret
   * @return the decrypted secret containing id and plaintext
   */
  DecryptedWebhookSecret decryptSecret(String secretId);
}
