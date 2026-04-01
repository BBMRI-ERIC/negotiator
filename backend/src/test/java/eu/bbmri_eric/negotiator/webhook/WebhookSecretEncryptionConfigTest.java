package eu.bbmri_eric.negotiator.webhook;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class WebhookSecretEncryptionConfigTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t", "\n"})
  void webhookTextEncryptorFactory_whenEncryptionEnabledAndMasterKeyBlank_throws(String masterKey) {
    WebhookSecretEncryptionProperties properties = new WebhookSecretEncryptionProperties();
    properties.setEnabled(true);
    properties.setMasterKey(masterKey);

    WebhookSecretEncryptionConfig config = new WebhookSecretEncryptionConfig();

    assertThrows(IllegalStateException.class, () -> config.webhookTextEncryptorFactory(properties));
  }
}
