package eu.bbmri_eric.negotiator.webhook.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class NewNegotiationWebhookMappingStrategy implements WebhookMappingStrategy<NewNegotiationEvent> {

  @Override
  public Class<NewNegotiationEvent> getSupportedEventType() {
    return NewNegotiationEvent.class;
  }

  @Override
  public Optional<WebhookPayloadEnvelope<?>> map(
      NewNegotiationEvent event, ObjectMapper objectMapper) {
    if (event.getCurrentState() == NegotiationState.DRAFT) {
      return Optional.empty();
    }
    NegotiationAddedWebhookEvent payload =
        new NegotiationAddedWebhookEvent(event.getNegotiationId(), event.getCurrentState());
    return Optional.of(
        new WebhookPayloadEnvelope<>(
            WebhookEventType.NEGOTIATION_ADDED,
            Instant.ofEpochMilli(event.getTimestamp()),
            payload));
  }

  @Override
  public Map<WebhookEventType, Class<?>> documentedPayloadTypes() {
    return Map.of(WebhookEventType.NEGOTIATION_ADDED, NegotiationAddedWebhookEvent.class);
  }
}
