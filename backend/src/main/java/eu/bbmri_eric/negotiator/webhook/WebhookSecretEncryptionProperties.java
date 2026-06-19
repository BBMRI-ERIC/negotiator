package eu.bbmri_eric.negotiator.webhook;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "negotiator.webhook.secret-encryption")
class WebhookSecretEncryptionProperties {
  private boolean enabled = true;
  private String masterKey = "";
  private String previousMasterKey = "";
}
