package eu.bbmri_eric.negotiator.webhook.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.WellKnownNegotiationStates;
import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
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
    String currentState = nameOf(event.getCurrentState());
    if (WellKnownNegotiationStates.DRAFT.equals(currentState)) {
      return Optional.empty();
    }
    NegotiationAddedWebhookEvent payload =
        new NegotiationAddedWebhookEvent(event.getNegotiationId(), currentState);
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

  /**
   * Reads a name off the creation event, which still deals in enums. Null-preserving, so a
   * Negotiation whose State is unset still yields a delivery rather than throwing.
   */
  private static String nameOf(Enum<?> stateOrEvent) {
    return stateOrEvent == null ? null : stateOrEvent.name();
  }
}
