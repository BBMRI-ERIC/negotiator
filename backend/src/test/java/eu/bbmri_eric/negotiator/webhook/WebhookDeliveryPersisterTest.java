package eu.bbmri_eric.negotiator.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryPersisterTest {

  @Mock private WebhookRepository webhookRepository;

  @Mock private ModelMapper modelMapper;

  private WebhookDeliveryPersister webhookDeliveryPersister;

  @BeforeEach
  void setUp() {
    webhookDeliveryPersister = new WebhookDeliveryPersister(webhookRepository, modelMapper);
  }

  @Test
  void persist_whenWebhookExists_addsDeliveryAndReturnsMappedDto() {
    Long webhookId = 1L;
    Webhook webhook = new Webhook("https://example.com/webhook", true, true);
    webhook.setId(webhookId);
    Delivery delivery = new Delivery("{\"test\":\"ok\"}", 200, WebhookEventType.CUSTOM);
    DeliveryDTO expected = new DeliveryDTO();

    when(webhookRepository.findById(webhookId)).thenReturn(Optional.of(webhook));
    when(webhookRepository.saveAndFlush(webhook)).thenReturn(webhook);
    when(modelMapper.map(any(Delivery.class), eq(DeliveryDTO.class))).thenReturn(expected);

    DeliveryDTO actual = webhookDeliveryPersister.persist(webhookId, delivery);

    assertSame(expected, actual);
    assertEquals(1, webhook.getDeliveries().size());
    assertEquals(webhookId, webhook.getDeliveries().get(0).getWebhookId());
    verify(webhookRepository).findById(webhookId);
    verify(webhookRepository).saveAndFlush(webhook);
  }

  @Test
  void persist_whenWebhookDoesNotExist_throwsEntityNotFoundException() {
    Long webhookId = 99L;
    Delivery delivery =
        new Delivery("{\"test\":\"not-found\"}", 500, "error", WebhookEventType.CUSTOM);

    when(webhookRepository.findById(webhookId)).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class, () -> webhookDeliveryPersister.persist(webhookId, delivery));
  }
}
