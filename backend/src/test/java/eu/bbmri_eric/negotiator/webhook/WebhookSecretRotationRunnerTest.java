package eu.bbmri_eric.negotiator.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@ExtendWith(MockitoExtension.class)
class WebhookSecretRotationRunnerTest {

  @Mock private WebhookSecretRepository webhookSecretRepository;
  @Mock private WebhookSecretServiceImpl webhookSecretService;
  @Mock private WebhookTextEncryptorFactory newEncryptorFactory;
  @Mock private TextEncryptor newEncryptor;

  @Test
  void constructor_whenEncryptionDisabled_throws() {
    WebhookSecretEncryptionProperties properties = new WebhookSecretEncryptionProperties();
    properties.setEnabled(false);

    assertThrows(
        IllegalStateException.class,
        () ->
            new WebhookSecretRotationRunner(
                webhookSecretRepository, webhookSecretService, newEncryptorFactory, properties));
  }

  @Test
  void constructor_whenPreviousMasterKeyMissing_throws() {
    WebhookSecretEncryptionProperties properties = new WebhookSecretEncryptionProperties();
    properties.setEnabled(true);
    properties.setPreviousMasterKey("");

    assertThrows(
        IllegalStateException.class,
        () ->
            new WebhookSecretRotationRunner(
                webhookSecretRepository, webhookSecretService, newEncryptorFactory, properties));
  }

  @Test
  void run_rotatesAndSavesAllDecryptableSecrets() {
    String previousMasterKey = "old-master-key";
    String firstSalt = "ab01cd23ef45ab01cd23ef45ab01cd23";
    String secondSalt = "cd23ef45ab01cd23ef45ab01cd23ef45";

    TextEncryptor firstOldEncryptor = Encryptors.text(previousMasterKey, firstSalt);
    TextEncryptor secondOldEncryptor = Encryptors.text(previousMasterKey, secondSalt);
    String encryptedOne = firstOldEncryptor.encrypt("plain-1");
    String encryptedTwo = secondOldEncryptor.encrypt("plain-2");

    WebhookSecret first = new WebhookSecret(encryptedOne, firstSalt);
    first.setId("secret-1");

    WebhookSecret second = new WebhookSecret(encryptedTwo, secondSalt);
    second.setId("secret-2");

    WebhookSecretEncryptionProperties properties = new WebhookSecretEncryptionProperties();
    properties.setEnabled(true);
    properties.setPreviousMasterKey(previousMasterKey);

    when(webhookSecretRepository.findAll()).thenReturn(List.of(first, second));
    when(newEncryptorFactory.create(any())).thenReturn(newEncryptor);
    when(newEncryptor.encrypt("plain-1")).thenReturn("new-enc-1");
    when(newEncryptor.encrypt("plain-2")).thenReturn("new-enc-2");

    WebhookSecretRotationRunner runner =
        new WebhookSecretRotationRunner(
            webhookSecretRepository, webhookSecretService, newEncryptorFactory, properties);

    runner.run();

    verify(newEncryptor).encrypt("plain-1");
    verify(newEncryptor).encrypt("plain-2");

    ArgumentCaptor<WebhookSecret> captor = ArgumentCaptor.forClass(WebhookSecret.class);
    verify(webhookSecretRepository, org.mockito.Mockito.times(2)).save(captor.capture());

    List<WebhookSecret> savedSecrets = captor.getAllValues();
    assertEquals("new-enc-1", savedSecrets.get(0).getEncryptedSecret());
    assertEquals("new-enc-2", savedSecrets.get(1).getEncryptedSecret());
  }

  @Test
  void run_whenSingleSecretFails_continuesWithRemainingSecrets() {
    String previousMasterKey = "old-master-key";
    String validSalt = "ab01cd23ef45ab01cd23ef45ab01cd23";
    String invalidSalt = "cd23ef45ab01cd23ef45ab01cd23ef45";

    TextEncryptor oldEncryptor = Encryptors.text(previousMasterKey, validSalt);
    String encryptedValid = oldEncryptor.encrypt("plain-ok");

    WebhookSecret valid = new WebhookSecret(encryptedValid, validSalt);
    valid.setId("secret-valid");

    WebhookSecret invalid = new WebhookSecret("not-decryptable-with-old-key", invalidSalt);
    invalid.setId("secret-invalid");

    WebhookSecretEncryptionProperties properties = new WebhookSecretEncryptionProperties();
    properties.setEnabled(true);
    properties.setPreviousMasterKey(previousMasterKey);

    when(webhookSecretRepository.findAll()).thenReturn(List.of(valid, invalid));
    when(newEncryptorFactory.create(any())).thenReturn(newEncryptor);
    when(newEncryptor.encrypt("plain-ok")).thenReturn("new-enc-ok");

    WebhookSecretRotationRunner runner =
        new WebhookSecretRotationRunner(
            webhookSecretRepository, webhookSecretService, newEncryptorFactory, properties);

    runner.run();

    verify(newEncryptor).encrypt("plain-ok");
    verify(webhookSecretRepository).save(any(WebhookSecret.class));
    verify(webhookSecretRepository, never()).save(eq(invalid));
  }
}
