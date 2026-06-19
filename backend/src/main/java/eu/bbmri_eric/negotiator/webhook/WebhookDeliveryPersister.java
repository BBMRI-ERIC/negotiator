package eu.bbmri_eric.negotiator.webhook;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class WebhookDeliveryPersister {

  private final WebhookRepository webhookRepository;
  private final ModelMapper modelMapper;

  @Transactional
  DeliveryDTO persist(Long webhookId, Delivery delivery) {
    Webhook webhook =
        webhookRepository
            .findById(webhookId)
            .orElseThrow(() -> new EntityNotFoundException(webhookId));
    webhook.addDelivery(delivery);
    webhook = webhookRepository.saveAndFlush(webhook);
    return modelMapper.map(webhook.getDeliveries().get(0), DeliveryDTO.class);
  }
}
