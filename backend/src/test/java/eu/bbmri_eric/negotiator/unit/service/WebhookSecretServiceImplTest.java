package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.webhook.DecryptedWebhookSecret;
import eu.bbmri_eric.negotiator.webhook.WebhookSecret;
import eu.bbmri_eric.negotiator.webhook.WebhookSecretRepository;
import eu.bbmri_eric.negotiator.webhook.WebhookSecretServiceImpl;
import eu.bbmri_eric.negotiator.webhook.WebhookTextEncryptorFactory;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.Encryptors;

@ExtendWith(MockitoExtension.class)
class WebhookSecretServiceImplTest {

  @Mock private WebhookSecretRepository webhookSecretRepository;

  private WebhookTextEncryptorFactory encryptorFactory;
  private WebhookSecretServiceImpl service;

  @BeforeEach
  void setUp() {
    encryptorFactory = salt -> Encryptors.noOpText();
    service = new WebhookSecretServiceImpl(webhookSecretRepository, encryptorFactory);
  }

  @Test
  void createSecret_returnsDecryptedSecretWithIdAndPlainText() {
    String plainTextSecret = webhookSecretForBytes(24);
    when(webhookSecretRepository.save(any(WebhookSecret.class)))
        .thenAnswer(
            invocation -> {
              WebhookSecret secret = invocation.getArgument(0);
              secret.setId("generated-uuid");
              return secret;
            });

    DecryptedWebhookSecret result = service.createSecret(plainTextSecret);

    assertEquals("generated-uuid", result.id());
    assertEquals(plainTextSecret, result.plainText());
  }

  @Test
  void createSecret_persistsEncryptedSecretAndPerSecretSalt() {
    String plainTextSecret = webhookSecretForBytes(24);
    when(webhookSecretRepository.save(any(WebhookSecret.class)))
        .thenAnswer(
            invocation -> {
              WebhookSecret secret = invocation.getArgument(0);
              secret.setId("test-id");
              return secret;
            });

    service.createSecret(plainTextSecret);

    ArgumentCaptor<WebhookSecret> captor = ArgumentCaptor.forClass(WebhookSecret.class);
    verify(webhookSecretRepository).save(captor.capture());
    assertNotNull(captor.getValue().getEncryptedSecret());
    assertFalse(captor.getValue().getEncryptedSecret().isBlank());
    assertNotNull(captor.getValue().getSalt());
    assertEquals(32, captor.getValue().getSalt().length());
    assertTrue(captor.getValue().getSalt().matches("[0-9a-f]+"));
  }

  @Test
  void validateSecret_withWhitespaceWrappedSecret_throwsIllegalArgumentException() {
    String secret = webhookSecretForBytes(24);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> service.validateSecret("  " + secret + "  "));
    assertEquals("Secret must start with whsec_", ex.getMessage());
  }

  @Test
  void validateSecret_withStandardBase64Secret_returnsOriginalSecret() {
    String standardSecret = webhookSecretForBytes(32);

    String validated = service.validateSecret(standardSecret);

    assertEquals(standardSecret, validated);
  }

  @Test
  void validateSecret_withOddBase64Secret_returnsOriginalSecret() {
    String oddSecret = webhookSecretForBytes(30);

    String validated = service.validateSecret(oddSecret);

    assertEquals(oddSecret, validated);
  }

  @Test
  void validateSecret_withMinimumAllowedDecodedBytes_returnsOriginalSecret() {
    String minBoundarySecret = webhookSecretForBytes(24);

    String validated = service.validateSecret(minBoundarySecret);

    assertEquals(minBoundarySecret, validated);
  }

  @Test
  void validateSecret_withMaximumAllowedDecodedBytes_returnsOriginalSecret() {
    String maxBoundarySecret = webhookSecretForBytes(64);

    String validated = service.validateSecret(maxBoundarySecret);

    assertEquals(maxBoundarySecret, validated);
  }

  @Test
  void validateSecret_withMaximumAllowedDecodedBytes_returnsOriginalSecret_custom() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[24];
    random.nextBytes(bytes);
    String maxBoundarySecret = "whsec_" + Base64.getEncoder().encodeToString(bytes);
    // assertEquals("aaaa", maxBoundarySecret);

    String validated = service.validateSecret(maxBoundarySecret);

    assertEquals(maxBoundarySecret, validated);
  }

  @Test
  void validateSecret_withUrlSafeBase64Secret_throwsIllegalArgumentException() {
    byte[] bytes = new byte[24];
    Arrays.fill(bytes, (byte) 0xFF);
    String urlSafeSecret = "whsec_" + Base64.getUrlEncoder().encodeToString(bytes);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.validateSecret(urlSafeSecret));

    assertEquals("Secret key material must be valid base64", ex.getMessage());
  }

  @Test
  void validateSecret_withBlankSecret_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> service.validateSecret("   "));
  }

  @Test
  void createSecret_withLegacySecretWithoutWhsecPrefix_throwsIllegalArgumentException() {
    String secretWithoutPrefix =
        Base64.getEncoder().encodeToString("a".repeat(24).getBytes(StandardCharsets.UTF_8));

    assertThrows(IllegalArgumentException.class, () -> service.createSecret(secretWithoutPrefix));
    verify(webhookSecretRepository, never()).save(any(WebhookSecret.class));
  }

  @Test
  void createSecret_withUppercasePrefix_throwsIllegalArgumentException() {
    String secret =
        "WHSEC_"
            + Base64.getEncoder().encodeToString("a".repeat(24).getBytes(StandardCharsets.UTF_8));

    assertThrows(IllegalArgumentException.class, () -> service.createSecret(secret));
    verify(webhookSecretRepository, never()).save(any(WebhookSecret.class));
  }

  @Test
  void createSecret_withInvalidBase64_throwsIllegalArgumentException() {
    String secret = "whsec_invalid-base64-$$$";

    assertThrows(IllegalArgumentException.class, () -> service.createSecret(secret));
    verify(webhookSecretRepository, never()).save(any(WebhookSecret.class));
  }

  @Test
  void createSecret_withDecodedSecretShorterThan24Bytes_throwsIllegalArgumentException() {
    String secret = webhookSecretForBytes(23);

    assertThrows(IllegalArgumentException.class, () -> service.createSecret(secret));
    verify(webhookSecretRepository, never()).save(any(WebhookSecret.class));
  }

  @Test
  void createSecret_withDecodedSecretLongerThan64Bytes_throwsIllegalArgumentException() {
    String secret = webhookSecretForBytes(65);

    assertThrows(IllegalArgumentException.class, () -> service.createSecret(secret));
    verify(webhookSecretRepository, never()).save(any(WebhookSecret.class));
  }

  @Test
  void createSecret_withWhitespaceWrappedSecret_throwsIllegalArgumentException() {
    String secret = webhookSecretForBytes(24);

    assertThrows(IllegalArgumentException.class, () -> service.createSecret("  " + secret + "  "));
    verify(webhookSecretRepository, never()).save(any(WebhookSecret.class));
  }

  @Test
  void deleteSecret_existingId_deletesSuccessfully() {
    when(webhookSecretRepository.existsById("secret-id")).thenReturn(true);

    service.deleteSecret("secret-id");

    verify(webhookSecretRepository).deleteById("secret-id");
  }

  @Test
  void deleteSecret_nonExistingId_throwsEntityNotFoundException() {
    when(webhookSecretRepository.existsById("missing")).thenReturn(false);

    assertThrows(EntityNotFoundException.class, () -> service.deleteSecret("missing"));
    verify(webhookSecretRepository, never()).deleteById(anyString());
  }

  @Test
  void decryptSecret_existingId_returnsPlainText() {
    var secret = new WebhookSecret("my-plain-secret", "ab01cd23ef45ab01cd23ef45ab01cd23");
    secret.setId("secret-id");
    when(webhookSecretRepository.findById("secret-id")).thenReturn(Optional.of(secret));

    DecryptedWebhookSecret result = service.decryptSecret("secret-id");

    assertEquals("secret-id", result.id());
    assertEquals("my-plain-secret", result.plainText());
  }

  @Test
  void decryptSecret_nonExistingId_throwsEntityNotFoundException() {
    when(webhookSecretRepository.findById("missing")).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> service.decryptSecret("missing"));
  }

  @Test
  void createSecret_withRealEncryptor_roundTripsCorrectly() {
    String plainTextSecret = webhookSecretForBytes(24);
    String password = "test-master-password";
    WebhookTextEncryptorFactory realEncryptorFactory = s -> Encryptors.text(password, s);
    var realService = new WebhookSecretServiceImpl(webhookSecretRepository, realEncryptorFactory);

    when(webhookSecretRepository.save(any(WebhookSecret.class)))
        .thenAnswer(
            invocation -> {
              WebhookSecret s = invocation.getArgument(0);
              s.setId("uuid-1");
              return s;
            });

    DecryptedWebhookSecret createdSecret = realService.createSecret(plainTextSecret);

    ArgumentCaptor<WebhookSecret> captor = ArgumentCaptor.forClass(WebhookSecret.class);
    verify(webhookSecretRepository).save(captor.capture());
    WebhookSecret savedSecret = captor.getValue();

    assertEquals(plainTextSecret, createdSecret.plainText());
    assertNotEquals(plainTextSecret, savedSecret.getEncryptedSecret());
    assertNotNull(savedSecret.getSalt());
    assertEquals(32, savedSecret.getSalt().length());

    when(webhookSecretRepository.findById(createdSecret.id())).thenReturn(Optional.of(savedSecret));
    DecryptedWebhookSecret decrypted = realService.decryptSecret(createdSecret.id());
    assertEquals(plainTextSecret, decrypted.plainText());
  }

  private static String webhookSecretForBytes(int bytesCount) {
    String material = "a".repeat(bytesCount);
    return "whsec_" + Base64.getEncoder().encodeToString(material.getBytes(StandardCharsets.UTF_8));
  }
}
