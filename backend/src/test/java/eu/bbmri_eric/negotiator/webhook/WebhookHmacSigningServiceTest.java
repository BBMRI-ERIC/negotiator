package eu.bbmri_eric.negotiator.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookHmacSigningServiceTest {

  @Mock private WebhookSecretService webhookSecretService;

  private WebhookHmacSigningService signingService;

  @BeforeEach
  void setUp() {
    signingService = new WebhookHmacSigningService(webhookSecretService);
  }

  @Test
  void createSignature_withoutSecretId_returnsEmpty() {
    var signature = signingService.createSignature("msg_123", 1700000000L, "{\"k\":\"v\"}", null);

    assertTrue(signature.isEmpty());
    verify(webhookSecretService, never()).decryptSecret(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void createSignature_withSecretId_returnsVersionedBase64Signature() {
    when(webhookSecretService.decryptSecret("secret-id"))
        .thenReturn(new DecryptedWebhookSecret("secret-id", webhookSecretForBytes(32)));

    var signature =
        signingService.createSignature("msg_123", 1700000000L, "{\"k\":\"v\"}", "secret-id");

    assertTrue(signature.isPresent());
    assertEquals("v1", signature.get().version().toString());
    assertFalse(signature.get().digest().isBlank());
    assertTrue(signature.get().toString().startsWith("v1,"));
    verify(webhookSecretService).decryptSecret("secret-id");
  }

  @Test
  void createSignature_sameInput_isDeterministic() {
    when(webhookSecretService.decryptSecret("secret-id"))
        .thenReturn(new DecryptedWebhookSecret("secret-id", webhookSecretForBytes(32)));

    WebhookSignature first =
        signingService
            .createSignature("msg_123", 1700000000L, "{\"k\":\"v\"}", "secret-id")
            .orElseThrow();
    WebhookSignature second =
        signingService
            .createSignature("msg_123", 1700000000L, "{\"k\":\"v\"}", "secret-id")
            .orElseThrow();

    assertEquals(first, second);
  }

  @Test
  void createSignature_differentPayload_changesSignature() {
    when(webhookSecretService.decryptSecret("secret-id"))
        .thenReturn(new DecryptedWebhookSecret("secret-id", webhookSecretForBytes(32)));

    WebhookSignature first =
        signingService
            .createSignature("msg_123", 1700000000L, "{\"k\":\"v\"}", "secret-id")
            .orElseThrow();
    WebhookSignature second =
        signingService
            .createSignature("msg_123", 1700000000L, "{\"k\":\"other\"}", "secret-id")
            .orElseThrow();

    assertFalse(first.equals(second));
  }

  @Test
  void createSignature_invalidSecretPrefix_throwsIllegalArgumentException() {
    when(webhookSecretService.decryptSecret("secret-id"))
        .thenReturn(new DecryptedWebhookSecret("secret-id", "invalid"));

    assertThrows(
        IllegalArgumentException.class,
        () -> signingService.createSignature("msg_123", 1700000000L, "{\"k\":\"v\"}", "secret-id"));
  }

  private static String webhookSecretForBytes(int bytesCount) {
    String material = "a".repeat(bytesCount);
    return "whsec_" + Base64.getEncoder().encodeToString(material.getBytes(StandardCharsets.UTF_8));
  }
}
