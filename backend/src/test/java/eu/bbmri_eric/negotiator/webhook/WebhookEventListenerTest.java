package eu.bbmri_eric.negotiator.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionEvent;
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

  @Mock private ObjectMapper eventObjectMapper;

  private WebhookEventListener webhookEventListener;

  @BeforeEach
  void setUp() {
    when(objectMapper.copy()).thenReturn(eventObjectMapper);
    when(eventObjectMapper.addMixIn(eq(ApplicationEvent.class), any()))
        .thenReturn(eventObjectMapper);
    webhookEventListener = new WebhookEventListener(webhookService, objectMapper);
  }

  @Test
  void onWebhookEvent_whenEventTypeIsNotDispatched_shouldIgnoreEvent()
      throws JsonProcessingException {
    ApplicationEvent unsupportedEvent = new ApplicationEvent(this) {};

    webhookEventListener.onWebhookEvent(unsupportedEvent);

    verify(eventObjectMapper, never()).writeValueAsString(any());
    verify(webhookService, never()).deliverToActiveWebhooks(any(), any());
  }

  @Test
  void onWebhookEvent_whenPayloadSerializationFails_shouldNotDeliverWebhook()
      throws JsonProcessingException {
    InformationSubmissionEvent event = new InformationSubmissionEvent(this, "negotiation-1");
    when(eventObjectMapper.writeValueAsString(event))
        .thenThrow(new JsonProcessingException("serialization failed") {});

    webhookEventListener.onWebhookEvent(event);

    verify(webhookService, never()).deliverToActiveWebhooks(any(), any());
  }

  @Test
  void onWebhookEvent_whenEventIsDispatched_shouldDeliverWebhook() throws JsonProcessingException {
    InformationSubmissionEvent event = new InformationSubmissionEvent(this, "negotiation-1");
    when(eventObjectMapper.writeValueAsString(event)).thenReturn("{\"id\":\"negotiation-1\"}");

    webhookEventListener.onWebhookEvent(event);

    verify(webhookService)
        .deliverToActiveWebhooks("{\"id\":\"negotiation-1\"}", "InformationSubmissionEvent");
  }
}
