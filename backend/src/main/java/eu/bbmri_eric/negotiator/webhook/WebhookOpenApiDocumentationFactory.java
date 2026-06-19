package eu.bbmri_eric.negotiator.webhook;

import eu.bbmri_eric.negotiator.webhook.event.PingWebhookEvent;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventDoc;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import eu.bbmri_eric.negotiator.webhook.event.WebhookMappingStrategy;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

/** Builds generated OpenAPI webhook documentation from mapped webhook event definitions. */
@Component
public class WebhookOpenApiDocumentationFactory {
  private static final String APPLICATION_JSON = "application/json";

  private final Map<WebhookEventType, Class<?>> documentedPayloadTypes;
  private final List<WebhookHeaderDocumentation> webhookHeaderDocumentation;

  public WebhookOpenApiDocumentationFactory(
      List<WebhookMappingStrategy<? extends ApplicationEvent>> mappingStrategies) {
    this.documentedPayloadTypes = buildDocumentedPayloadTypes(mappingStrategies);
    this.webhookHeaderDocumentation = discoverWebhookHeaderDocumentation();
  }

  /**
   * Builds webhook path documentation and registers the related schemas in components.
   *
   * @param components mutable OpenAPI components container
   * @return immutable map of OpenAPI webhook path items keyed by event type value
   */
  public Map<String, PathItem> buildWebhookPaths(Components components) {
    Map<String, PathItem> webhookPaths = new TreeMap<>();
    for (Map.Entry<WebhookEventType, Class<?>> definition : documentedPayloadTypes.entrySet()) {
      WebhookEventType eventType = definition.getKey();
      Class<?> dataType = definition.getValue();
      String dataSchemaName = registerDataSchema(components, dataType);
      String envelopeSchemaName =
          registerEnvelopeSchema(components, eventType, dataType, dataSchemaName);
      webhookPaths.put(
          eventType.value(),
          new PathItem().post(buildWebhookOperation(eventType, dataType, envelopeSchemaName)));
    }
    return Collections.unmodifiableMap(webhookPaths);
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

  private String registerDataSchema(Components components, Class<?> dataType) {
    ResolvedSchema resolvedSchema =
        ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(dataType));
    if (resolvedSchema.referencedSchemas != null) {
      resolvedSchema.referencedSchemas.forEach(components::addSchemas);
    }

    String dataSchemaName = dataType.getSimpleName();
    Schema<?> dataSchema = resolvedSchema.schema;
    if (dataSchema == null && resolvedSchema.referencedSchemas != null) {
      dataSchema = resolvedSchema.referencedSchemas.get(dataSchemaName);
    }
    if (dataSchema == null) {
      dataSchema = new ObjectSchema();
    }

    dataSchema.setName(dataSchemaName);
    components.addSchemas(dataSchemaName, dataSchema);
    return dataSchemaName;
  }

  private String registerEnvelopeSchema(
      Components components, WebhookEventType eventType, Class<?> dataType, String dataSchemaName) {
    String envelopeSchemaName = dataType.getSimpleName() + "Envelope";
    ObjectSchema envelopeSchema = new ObjectSchema();
    envelopeSchema.setDescription(
        "Outbound webhook envelope for event type '" + eventType.value() + "'.");

    StringSchema eventTypeSchema = new StringSchema();
    eventTypeSchema.description("Semantic webhook event type");
    eventTypeSchema.example(eventType.value());
    eventTypeSchema.setEnum(List.of(eventType.value()));
    envelopeSchema.addProperty("type", eventTypeSchema);

    envelopeSchema.addProperty(
        "timestamp",
        new StringSchema()
            .description("UTC timestamp describing when the source event happened")
            .format("date-time")
            .example("2026-04-24T07:12:30Z"));
    envelopeSchema.addProperty(
        "data", new Schema<>().$ref("#/components/schemas/" + dataSchemaName));
    envelopeSchema.setRequired(List.of("type", "timestamp", "data"));
    components.addSchemas(envelopeSchemaName, envelopeSchema);
    return envelopeSchemaName;
  }

  private Operation buildWebhookOperation(
      WebhookEventType eventType, Class<?> dataType, String envelopeSchemaName) {
    Operation operation =
        new Operation()
            .summary(resolveWebhookSummary(eventType, dataType))
            .description(resolveWebhookDescription(eventType, dataType))
            .requestBody(
                new RequestBody()
                    .required(true)
                    .content(
                        new Content()
                            .addMediaType(
                                APPLICATION_JSON,
                                new MediaType()
                                    .schema(
                                        new Schema<>()
                                            .$ref("#/components/schemas/" + envelopeSchemaName)))))
            .responses(
                new ApiResponses()
                    .addApiResponse("200", new ApiResponse().description("Webhook accepted"))
                    .addApiResponse("400", new ApiResponse().description("Invalid payload")));

    webhookHeaderDocumentation.forEach(
        headerDocumentation ->
            operation.addParametersItem(webhookHeaderParameter(headerDocumentation)));
    return operation;
  }

  private Parameter webhookHeaderParameter(WebhookHeaderDocumentation headerDocumentation) {
    Parameter parameter =
        new Parameter()
            .in("header")
            .name(headerDocumentation.name())
            .description(headerDocumentation.description())
            .required(headerDocumentation.required())
            .schema(new StringSchema());

    if (!headerDocumentation.example().isBlank()) {
      parameter.example(headerDocumentation.example());
    }
    return parameter;
  }

  private static WebhookHeaderDocumentation toWebhookHeaderDocumentation(Field headerField) {
    String fieldName = headerField.getName();
    WebhookHeaderDoc headerDoc = headerField.getAnnotation(WebhookHeaderDoc.class);
    if (headerDoc == null) {
      throw new IllegalStateException(
          "Missing @WebhookHeaderDoc on webhook header constant: " + fieldName);
    }

    String headerName;
    try {
      headerName = (String) headerField.get(null);
    } catch (IllegalAccessException exception) {
      throw new IllegalStateException(
          "Unable to access webhook header constant: " + fieldName, exception);
    }

    return new WebhookHeaderDocumentation(
        headerName, headerDoc.description(), headerDoc.required(), headerDoc.example());
  }

  private static List<WebhookHeaderDocumentation> discoverWebhookHeaderDocumentation() {
    return Arrays.stream(WebhookHeaders.class.getDeclaredFields())
        .filter(WebhookOpenApiDocumentationFactory::isWebhookHeaderField)
        .map(WebhookOpenApiDocumentationFactory::toWebhookHeaderDocumentation)
        .toList();
  }

  private static boolean isWebhookHeaderField(Field field) {
    int modifiers = field.getModifiers();
    return Modifier.isStatic(modifiers)
        && Modifier.isFinal(modifiers)
        && String.class.equals(field.getType());
  }

  private record WebhookHeaderDocumentation(
      String name, String description, boolean required, String example) {}

  private String resolveWebhookSummary(WebhookEventType eventType, Class<?> dataType) {
    WebhookEventDoc eventDoc = dataType.getAnnotation(WebhookEventDoc.class);
    if (eventDoc == null || eventDoc.summary().isBlank()) {
      return "Webhook event: " + eventType.value();
    }
    return eventDoc.summary();
  }

  private String resolveWebhookDescription(WebhookEventType eventType, Class<?> dataType) {
    WebhookEventDoc eventDoc = dataType.getAnnotation(WebhookEventDoc.class);
    if (eventDoc == null || eventDoc.description().isBlank()) {
      return "Outbound webhook event for '" + eventType.value() + "'.";
    }
    return eventDoc.description();
  }
}
