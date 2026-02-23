package eu.bbmri_eric.negotiator.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionEvent;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventEnvelope;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventMapper;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;

@ExtendWith(MockitoExtension.class)
class WebhookEventListenerTest {

  @Mock private WebhookService webhookService;

  @Mock private ObjectMapper objectMapper;

  @Mock private WebhookEventMapper webhookEventMapper;

  private WebhookEventListener webhookEventListener;

  @BeforeEach
  void setUp() {
    webhookEventListener =
        new WebhookEventListener(webhookService, objectMapper, webhookEventMapper);
  }

  @Test
  void onWebhookEvent_whenEventTypeIsNotDispatched_shouldIgnoreEvent()
      throws JsonProcessingException {
    ApplicationEvent unsupportedEvent = new ApplicationEvent(this) {};
    when(webhookEventMapper.map(unsupportedEvent)).thenReturn(Optional.empty());

    webhookEventListener.onWebhookEvent(unsupportedEvent);

    verify(objectMapper, never()).writeValueAsString(any());
    verify(webhookService, never()).deliverToActiveWebhooks(any(), any());
  }

  @Test
  void onWebhookEvent_whenPayloadSerializationFails_shouldNotDeliverWebhook()
      throws JsonProcessingException {
    InformationSubmissionEvent event = new InformationSubmissionEvent(this, "negotiation-1");
    WebhookEventEnvelope<Map<String, String>> envelope =
        new WebhookEventEnvelope<>(
            WebhookEventType.NEGOTIATION_INFO_UPDATED,
            Instant.parse("2026-01-01T00:00:00Z"),
            Map.of("negotiationId", "negotiation-1"));
    when(webhookEventMapper.map(event)).thenReturn(Optional.of(envelope));
    when(objectMapper.writeValueAsString(envelope))
        .thenThrow(new JsonProcessingException("serialization failed") {});

    webhookEventListener.onWebhookEvent(event);

    verify(webhookService, never()).deliverToActiveWebhooks(any(), any());
  }

  @Test
  void onWebhookEvent_whenEventIsDispatched_shouldDeliverWebhook() throws JsonProcessingException {
    InformationSubmissionEvent event = new InformationSubmissionEvent(this, "negotiation-1");
    WebhookEventEnvelope<Map<String, String>> envelope =
        new WebhookEventEnvelope<>(
            WebhookEventType.NEGOTIATION_INFO_UPDATED,
            Instant.parse("2026-01-01T00:00:00Z"),
            Map.of("negotiationId", "negotiation-1"));
    when(webhookEventMapper.map(event)).thenReturn(Optional.of(envelope));
    when(objectMapper.writeValueAsString(envelope))
        .thenReturn("{\"data\":{\"negotiationId\":\"negotiation-1\"}}");

    webhookEventListener.onWebhookEvent(event);

    verify(webhookService)
        .deliverToActiveWebhooks(
            "{\"data\":{\"negotiationId\":\"negotiation-1\"}}",
            WebhookEventType.NEGOTIATION_INFO_UPDATED);
  }
}
