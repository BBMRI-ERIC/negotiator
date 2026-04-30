package eu.bbmri_eric.negotiator.webhook;

import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@CommonsLog
@Component
@Profile("rotate-secrets")
public class WebhookSecretRotationRunner implements CommandLineRunner {

  private final WebhookSecretRepository webhookSecretRepository;
  private final WebhookSecretServiceImpl webhookSecretService;
  private final WebhookTextEncryptorFactory newEncryptorFactory;
  private final WebhookTextEncryptorFactory oldEncryptorFactory;

  public WebhookSecretRotationRunner(
      WebhookSecretRepository webhookSecretRepository,
      WebhookSecretServiceImpl webhookSecretService,
      WebhookTextEncryptorFactory webhookTextEncryptorFactory,
      WebhookSecretEncryptionProperties properties) {
    this.webhookSecretRepository = webhookSecretRepository;
    this.webhookSecretService = webhookSecretService;

    if (!properties.isEnabled()) {
      throw new IllegalStateException(
          "Secret rotation requires negotiator.webhook.secret-encryption.enabled=true");
    }
    if (StringUtils.isBlank(properties.getPreviousMasterKey())) {
      throw new IllegalStateException(
          "Secret rotation requires negotiator.webhook.secret-encryption.previous-master-key");
    }

    this.oldEncryptorFactory = salt -> Encryptors.text(properties.getPreviousMasterKey(), salt);
    this.newEncryptorFactory = webhookTextEncryptorFactory;
  }

  @Override
  @Transactional
  public void run(String... args) {
    List<WebhookSecret> secrets = webhookSecretRepository.findAll();
    log.info("Starting secret rotation for %d secrets".formatted(secrets.size()));
    int rotated = 0;
    for (WebhookSecret secret : secrets) {
      try {
        String plainText =
            oldEncryptorFactory.create(secret.getSalt()).decrypt(secret.getEncryptedSecret());
        String newSalt = webhookSecretService.generateRandomSalt();
        String reEncrypted = newEncryptorFactory.create(newSalt).encrypt(plainText);
        secret.setEncryptedSecret(reEncrypted);
        secret.setSalt(newSalt);
        webhookSecretRepository.save(secret);
        rotated++;
      } catch (Exception e) {
        log.error("Failed to rotate secret %s: %s".formatted(secret.getId(), e.getMessage()));
      }
    }
    log.info("Secret rotation complete. Rotated %d/%d secrets".formatted(rotated, secrets.size()));
  }
}
