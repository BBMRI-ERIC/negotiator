package eu.bbmri_eric.negotiator.webhook;

import static org.mockito.Mockito.verify;

import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryDispatcherTest {

  @Mock private WebhookService webhookService;

  private WebhookDeliveryDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    dispatcher = new WebhookDeliveryDispatcher(webhookService);
  }

  @Test
  void scheduleDelivery_delegatesToWebhookService() {
    dispatcher.scheduleDelivery(
        1L, "{\"key\":\"value\"}", WebhookEventType.NEGOTIATION_INFO_UPDATED);

    verify(webhookService)
        .deliver("{\"key\":\"value\"}", WebhookEventType.NEGOTIATION_INFO_UPDATED, 1L);
  }
}
