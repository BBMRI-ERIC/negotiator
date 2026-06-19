package eu.bbmri_eric.negotiator.webhook.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
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
    if (event.getFromState() == NegotiationState.DRAFT
        && event.getToState() == NegotiationState.SUBMITTED) {
      NegotiationAddedWebhookEvent payload =
          new NegotiationAddedWebhookEvent(event.getNegotiationId(), event.getToState());
      return Optional.of(
          new WebhookPayloadEnvelope<>(
              WebhookEventType.NEGOTIATION_ADDED,
              Instant.ofEpochMilli(event.getTimestamp()),
              payload));
    }

    NegotiationStateUpdatedWebhookEvent payload =
        new NegotiationStateUpdatedWebhookEvent(
            event.getNegotiationId(), event.getFromState(), event.getToState(), event.getEvent());
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
}
