package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;

/**
 * Reads a Wiring row's {@code params} into the strategy's declared params type, for {@link
 * GuardRegistry} and {@link ActionRegistry}.
 *
 * <p>The reader enables {@code FAIL_ON_UNKNOWN_PROPERTIES} itself, because Spring Boot disables it
 * on the injected {@link ObjectMapper}. Without it a misspelled field would silently bind the
 * type's default.
 *
 * <p>Every refusal is an {@link InvalidGraphException} during Definition Compilation, never at
 * Event Firing, so the administrator publishing a broken row sees the error.
 */
final class WiringConfigurationReader {

  /** A JSON {@code null} blob means the same as an absent column. */
  private static final String JSON_NULL = "null";

  /**
   * Accepted as "no params" only for a strategy that takes none. For any other strategy it is read
   * as a normal object.
   */
  private static final String EMPTY_JSON_OBJECT = "{}";

  private final String strategyNoun;
  private final ObjectReader reader;

  static WiringConfigurationReader forGuards(ObjectMapper objectMapper) {
    return new WiringConfigurationReader("Guard", objectMapper);
  }

  static WiringConfigurationReader forActions(ObjectMapper objectMapper) {
    return new WiringConfigurationReader("Action", objectMapper);
  }

  private WiringConfigurationReader(String strategyNoun, ObjectMapper objectMapper) {
    this.strategyNoun = strategyNoun;
    this.reader = objectMapper.reader().with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  /**
   * Reads one Wiring row's configuration into the type its strategy declared.
   *
   * @throws InvalidGraphException if the row carries configuration for a strategy that takes none,
   *     carries none for a strategy that needs some, or carries a blob that does not fit the
   *     declared type, including an unrecognised field
   */
  <P> P read(String typeKey, Class<P> paramsType, String paramsJson) {
    if (paramsType == NoParams.class) {
      if (carriesNothing(paramsJson) || EMPTY_JSON_OBJECT.equals(paramsJson.strip())) {
        return paramsType.cast(NoParams.INSTANCE);
      }
      throw new InvalidGraphException(
          ("%s '%s' takes no params, but its Wiring row carries %s."
                  + " A strategy that takes none accepts only an absent params column, null or {}.")
              .formatted(strategyNoun, typeKey, paramsJson));
    }
    if (carriesNothing(paramsJson)) {
      throw new InvalidGraphException(
          "%s '%s' declares params of type %s but its Wiring row carries none."
              .formatted(strategyNoun, typeKey, paramsType.getSimpleName()));
    }
    try {
      return paramsType.cast(reader.forType(paramsType).readValue(paramsJson));
    } catch (JsonProcessingException e) {
      throw new InvalidGraphException(
          "%s '%s' could not read its params as %s: %s"
              .formatted(strategyNoun, typeKey, paramsType.getSimpleName(), paramsJson),
          e);
    }
  }

  /**
   * {@code params} is {@code JSONB} mapped to a {@code String}, so an absent column arrives as
   * {@code null} and a JSON {@code null} arrives as the string {@code "null"}.
   */
  private static boolean carriesNothing(String paramsJson) {
    return paramsJson == null || paramsJson.isBlank() || JSON_NULL.equals(paramsJson.strip());
  }
}
