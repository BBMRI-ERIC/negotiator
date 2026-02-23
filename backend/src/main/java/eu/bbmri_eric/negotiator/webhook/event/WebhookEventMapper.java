package eu.bbmri_eric.negotiator.webhook.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionEvent;
import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

/**
 * Maps internal Spring {@link ApplicationEvent} instances to stable webhook event envelopes.
 * The mapper keeps the external webhook contract stable by converting supported application
 * events into dedicated webhook DTO records and semantic event type names.
 */
@Component
public class WebhookEventMapper {
  private final ObjectMapper objectMapper;
  private final Map<Class<? extends ApplicationEvent>, WebhookEventDefinition<?, ?>>
      eventDefinitions;

  /**
   * Creates a mapper using a dedicated {@link ObjectMapper} copy configured for webhook mapping.
   *
   * @param objectMapper the base object mapper to copy and configure
   */
  public WebhookEventMapper(ObjectMapper objectMapper) {
    this.objectMapper =
        objectMapper.copy().addMixIn(ApplicationEvent.class, ApplicationEventMixin.class);
    this.eventDefinitions =
        Map.of(
            InformationSubmissionEvent.class,
            WebhookEventDefinition.of(
                InformationSubmissionEvent.class,
                NegotiationInfoUpdatedWebhookEvent.class,
                WebhookEventType.NEGOTIATION_INFO_UPDATED),
            NegotiationStateChangeEvent.class,
            WebhookEventDefinition.of(
                NegotiationStateChangeEvent.class,
                NegotiationStateUpdatedWebhookEvent.class,
                WebhookEventType.NEGOTIATION_STATE_UPDATED),
            NewNegotiationEvent.class,
            WebhookEventDefinition.of(
                NewNegotiationEvent.class,
                NegotiationAddedWebhookEvent.class,
                WebhookEventType.NEGOTIATION_ADDED),
            NewResourcesAddedEvent.class,
            WebhookEventDefinition.of(
                NewResourcesAddedEvent.class,
                NegotiationResourceUpdatedWebhookEvent.class,
                WebhookEventType.NEGOTIATION_RESOURCE_ADDED),
            NewPostEvent.class,
            WebhookEventDefinition.of(
                NewPostEvent.class,
                NegotiationPostAddedWebhookEvent.class,
                WebhookEventType.NEGOTIATION_POST_ADDED),
            ResourceStateChangeEvent.class,
            WebhookEventDefinition.of(
                ResourceStateChangeEvent.class,
                NegotiationResourceStateUpdatedWebhookEvent.class,
                WebhookEventType.NEGOTIATION_RESOURCE_STATE_UPDATED));
  }

  /**
   * Converts a supported application event into a stable webhook envelope.
   *
   * @param event the application event to map
   * @return an envelope when the event type is supported, otherwise an empty optional
   */
  public Optional<WebhookEventEnvelope<?>> map(ApplicationEvent event) {
    WebhookEventDefinition<?, ?> eventDefinition = eventDefinitions.get(event.getClass());
    if (eventDefinition == null) {
      return Optional.empty();
    }
    return Optional.of(eventDefinition.toEnvelope(event, objectMapper));
  }

  private record WebhookEventDefinition<S extends ApplicationEvent, T>(
      Class<S> sourceType, Class<T> dataType, String eventType) {

    private static <S extends ApplicationEvent, T> WebhookEventDefinition<S, T> of(
        Class<S> sourceType, Class<T> dataType, String eventType) {
      return new WebhookEventDefinition<>(sourceType, dataType, eventType);
    }

    private WebhookEventEnvelope<T> toEnvelope(ApplicationEvent sourceEvent, ObjectMapper mapper) {
      S event = sourceType.cast(sourceEvent);
      T data = mapper.convertValue(event, dataType);
      return new WebhookEventEnvelope<>(
          eventType, Instant.ofEpochMilli(event.getTimestamp()), data);
    }
  }

  private static final class ApplicationEventMixin {
    @JsonIgnore
    public Object getSource() {
      return null;
    }

    @JsonIgnore
    public long getTimestamp() {
      return 0L;
    }
  }
}
