package eu.bbmri_eric.negotiator.webhook.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.WellKnownNegotiationStates;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class NegotiationStateChangeWebhookMappingStrategy
    implements WebhookMappingStrategy<NegotiationStateChangeEvent> {

  @Override
  public Class<NegotiationStateChangeEvent> getSupportedEventType() {
    return NegotiationStateChangeEvent.class;
  }

  @Override
  public Optional<WebhookPayloadEnvelope<?>> map(
      NegotiationStateChangeEvent event, ObjectMapper objectMapper) {
    String fromState = nameOf(event.getFromState());
    String toState = nameOf(event.getToState());
    if (WellKnownNegotiationStates.DRAFT.equals(fromState)
        && WellKnownNegotiationStates.SUBMITTED.equals(toState)) {
      NegotiationAddedWebhookEvent payload =
          new NegotiationAddedWebhookEvent(event.getNegotiationId(), toState);
      return Optional.of(
          new WebhookPayloadEnvelope<>(
              WebhookEventType.NEGOTIATION_ADDED,
              Instant.ofEpochMilli(event.getTimestamp()),
              payload));
    }

    NegotiationStateUpdatedWebhookEvent payload =
        new NegotiationStateUpdatedWebhookEvent(
            event.getNegotiationId(), fromState, toState, nameOf(event.getEvent()));
    return Optional.of(
        new WebhookPayloadEnvelope<>(
            WebhookEventType.NEGOTIATION_STATE_UPDATED,
            Instant.ofEpochMilli(event.getTimestamp()),
            payload));
  }

  @Override
  public Map<WebhookEventType, Class<?>> documentedPayloadTypes() {
    return Map.of(
        WebhookEventType.NEGOTIATION_ADDED,
        NegotiationAddedWebhookEvent.class,
        WebhookEventType.NEGOTIATION_STATE_UPDATED,
        NegotiationStateUpdatedWebhookEvent.class);
  }

  /**
   * Reads a name off the change event, which still deals in enums. Null-preserving so that adding
   * the translation cannot turn a null into an exception; the only producer builds all three values
   * with {@code valueOf} and so cannot currently pass one.
   */
  private static String nameOf(Enum<?> stateOrEvent) {
    return stateOrEvent == null ? null : stateOrEvent.name();
  }
}
