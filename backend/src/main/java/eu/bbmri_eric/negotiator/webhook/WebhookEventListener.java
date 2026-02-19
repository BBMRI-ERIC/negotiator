package eu.bbmri_eric.negotiator.webhook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionEvent;
import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import java.util.Set;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for specific application events and dispatches them to active webhooks. This listener
 * serializes supported events and delivers them to all configured webhooks using the {@link
 * WebhookService}. Only events listed in {@code DISPATCHED_EVENTS} are processed.
 */
@Component
@CommonsLog
class WebhookEventListener {
  private static final Set<Class<? extends ApplicationEvent>> DISPATCHED_EVENTS =
      Set.of(
          InformationSubmissionEvent.class,
          NegotiationStateChangeEvent.class,
          NewNegotiationEvent.class,
          NewPostEvent.class,
          ResourceStateChangeEvent.class);

  private final WebhookService webhookService;
  private final ObjectMapper eventObjectMapper;

  WebhookEventListener(WebhookService webhookService, ObjectMapper objectMapper) {
    this.webhookService = webhookService;
    this.eventObjectMapper =
        objectMapper.copy().addMixIn(ApplicationEvent.class, ApplicationEventMixin.class);
  }

  /**
   * Handles application events asynchronously and dispatches them to webhooks if supported.
   *
   * @param event the application event to process
   */
  @Async
  @TransactionalEventListener(fallbackExecution = true)
  void onWebhookEvent(ApplicationEvent event) {
    if (!DISPATCHED_EVENTS.contains(event.getClass())) {
      return;
    }
    dispatch(event);
  }

  private void dispatch(ApplicationEvent event) {
    String payload = serializePayload(event);
    if (payload == null) {
      return;
    }

    webhookService.deliverToActiveWebhooks(payload, eventName(event));
  }

  private String serializePayload(ApplicationEvent event) {
    try {
      return eventObjectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      log.error(
          "Failed to serialize webhook payload for event: %s"
              .formatted(event.getClass().getSimpleName()),
          e);
      return null;
    }
  }

  private String eventName(ApplicationEvent event) {
    return event.getClass().getSimpleName();
  }

  /** Jackson mixin to ignore the source property when serializing {@link ApplicationEvent}. */
  private static final class ApplicationEventMixin {
    @JsonIgnore
    public Object getSource() {
      return null;
    }
  }
}
