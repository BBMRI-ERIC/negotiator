package eu.bbmri_eric.negotiator.webhook;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;

@Configuration
class WebhookSecretEncryptionConfig {

  @Bean
  WebhookTextEncryptorFactory webhookTextEncryptorFactory(
      WebhookSecretEncryptionProperties properties) {
    if (!properties.isEnabled()) {
      return salt -> Encryptors.noOpText();
    }

    if (StringUtils.isBlank(properties.getMasterKey())) {
      throw new IllegalStateException(
          "Webhook encryption is enabled but negotiator.webhook.secret-encryption.master-key"
              + " is blank. Set this value or explicitly disable encryption with"
              + " negotiator.webhook.secret-encryption.enabled=false.");
    }

    return salt -> Encryptors.text(properties.getMasterKey(), salt);
  }
}
