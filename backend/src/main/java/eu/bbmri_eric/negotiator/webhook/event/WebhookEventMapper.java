package eu.bbmri_eric.negotiator.webhook.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

/**
 * Maps internal Spring {@link ApplicationEvent} instances to stable webhook event envelopes. The
 * mapper keeps the external webhook contract stable by converting supported application events into
 * dedicated webhook DTO records and semantic event type names.
 */
@Component
public class WebhookEventMapper {
  private final ObjectMapper objectMapper;
  private final Map<
          Class<? extends ApplicationEvent>, WebhookMappingStrategy<? extends ApplicationEvent>>
      strategyRegistry;
  private final Map<WebhookEventType, Class<?>> documentedPayloadTypes;

  /**
   * Creates a mapper using a dedicated {@link ObjectMapper} copy configured for webhook mapping.
   *
   * @param objectMapper the base object mapper to copy and configure
   */
  public WebhookEventMapper(
      ObjectMapper objectMapper,
      List<WebhookMappingStrategy<? extends ApplicationEvent>> mappingStrategies) {
    this.objectMapper =
        objectMapper.copy().addMixIn(ApplicationEvent.class, ApplicationEventMixin.class);
    this.strategyRegistry = buildStrategyRegistry(mappingStrategies);
    this.documentedPayloadTypes = buildDocumentedPayloadTypes(mappingStrategies);
  }

  /**
   * Converts a supported application event into a stable webhook envelope.
   *
   * @param event the application event to map
   * @return an envelope when the event type is supported, otherwise an empty optional
   */
  public Optional<WebhookPayloadEnvelope<?>> map(ApplicationEvent event) {
    WebhookMappingStrategy<? extends ApplicationEvent> strategy =
        strategyRegistry.get(event.getClass());
    if (strategy == null) {
      return Optional.empty();
    }
    return mapWithStrategy(strategy, event);
  }

  private <T extends ApplicationEvent> Optional<WebhookPayloadEnvelope<?>> mapWithStrategy(
      WebhookMappingStrategy<T> strategy, ApplicationEvent event) {
    return strategy.map(strategy.getSupportedEventType().cast(event), objectMapper);
  }

  /**
   * Lists all webhook event types and payload DTO classes that should be documented in OpenAPI.
   *
   * @return immutable map of webhook event type to payload DTO class
   */
  public Map<WebhookEventType, Class<?>> documentedPayloadTypes() {
    return documentedPayloadTypes;
  }

  private static Map<
          Class<? extends ApplicationEvent>, WebhookMappingStrategy<? extends ApplicationEvent>>
      buildStrategyRegistry(
          List<WebhookMappingStrategy<? extends ApplicationEvent>> mappingStrategies) {
    Map<Class<? extends ApplicationEvent>, WebhookMappingStrategy<? extends ApplicationEvent>>
        registry = new HashMap<>();
    for (WebhookMappingStrategy<? extends ApplicationEvent> strategy : mappingStrategies) {
      Class<? extends ApplicationEvent> eventType = strategy.getSupportedEventType();
      WebhookMappingStrategy<? extends ApplicationEvent> existing =
          registry.putIfAbsent(eventType, strategy);
      if (existing != null) {
        throw new IllegalStateException(
            "Multiple webhook mapping strategies configured for event type: "
                + eventType.getName()
                + ". Found: "
                + existing.getClass().getName()
                + " and "
                + strategy.getClass().getName());
      }
    }
    return Map.copyOf(registry);
  }

  private static Map<WebhookEventType, Class<?>> buildDocumentedPayloadTypes(
      List<WebhookMappingStrategy<? extends ApplicationEvent>> mappingStrategies) {
    Map<WebhookEventType, Class<?>> payloadTypes = new EnumMap<>(WebhookEventType.class);
    for (WebhookMappingStrategy<? extends ApplicationEvent> strategy : mappingStrategies) {
      for (Map.Entry<WebhookEventType, Class<?>> payloadEntry :
          strategy.documentedPayloadTypes().entrySet()) {
        Class<?> existingClass =
            payloadTypes.putIfAbsent(payloadEntry.getKey(), payloadEntry.getValue());
        if (existingClass != null && !existingClass.equals(payloadEntry.getValue())) {
          throw new IllegalStateException(
              "Conflicting documented payload type for webhook event: "
                  + payloadEntry.getKey().value());
        }
      }
    }
    payloadTypes.put(WebhookEventType.PING, PingWebhookEvent.class);
    return Map.copyOf(payloadTypes);
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
