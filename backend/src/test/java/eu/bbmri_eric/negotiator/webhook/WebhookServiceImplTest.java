package eu.bbmri_eric.negotiator.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import eu.bbmri_eric.negotiator.webhook.event.WebhookPayloadEnvelope;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class WebhookServiceImplTest {

  @Mock private WebhookRepository webhookRepository;
  @Mock private DeliveryRepository deliveryRepository;
  @Mock private ModelMapper modelMapper;
  @Mock private WebhookDeliveryPersister webhookDeliveryPersister;
  @Mock private WebhookSecretService webhookSecretService;
  @Mock private WebhookSigningService webhookSigningService;
  @Mock private RestTemplate secureRestTemplate;
  @Mock private RestTemplate insecureRestTemplate;

  private WebhookServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new WebhookServiceImpl(
            webhookRepository,
            deliveryRepository,
            modelMapper,
            new ObjectMapper(),
            webhookDeliveryPersister,
            webhookSecretService,
            webhookSigningService,
            secureRestTemplate,
            insecureRestTemplate);
  }

  @Test
  void deliver_whenPayloadIsInvalidJson_throwsIllegalArgumentException() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.deliver("not-valid-json", WebhookEventType.PING, 1L));

    assertEquals("Content is not a valid JSON", ex.getMessage());
    verifyNoInteractions(
        webhookRepository,
        deliveryRepository,
        webhookDeliveryPersister,
        secureRestTemplate,
        insecureRestTemplate);
  }

  @Test
  void ping_whenPayloadEnvelopeIsNotSerializable_throwsIllegalStateException() {
    Long webhookId = 1L;
    var payloadEnvelope =
        new WebhookPayloadEnvelope<>(
            WebhookEventType.PING, Instant.now(), new NonSerializablePing());

    try (MockedStatic<?> mockedPing = mockStatic(WebhookPayloadEnvelope.class)) {
      mockedPing
          .when(() -> WebhookPayloadEnvelope.ping(eq(webhookId), any(Instant.class)))
          .thenReturn(payloadEnvelope);

      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> service.ping(webhookId));

      assertEquals("Could not serialize webhook payload", ex.getMessage());
      assertInstanceOf(JsonProcessingException.class, ex.getCause());
    }

    verifyNoInteractions(
        webhookRepository,
        deliveryRepository,
        webhookDeliveryPersister,
        secureRestTemplate,
        insecureRestTemplate);
  }

  @SuppressWarnings("unused")
  private static final class NonSerializablePing {
    public String getWebhookId() {
      throw new UnsupportedOperationException("Cannot serialize ping payload");
    }
  }
}
