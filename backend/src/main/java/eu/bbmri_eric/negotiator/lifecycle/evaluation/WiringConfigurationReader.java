package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;

/**
 * The one place a Wiring row's {@code params} becomes a strategy's declared params type, shared by
 * {@link GuardRegistry} and {@link ActionRegistry} and strict by construction.
 *
 * <p><b>Strictness is set here rather than inherited.</b> The reader enables {@code
 * FAIL_ON_UNKNOWN_PROPERTIES} on itself, whatever the injected {@link ObjectMapper} was configured
 * with, because Spring Boot's auto-configuration disables that feature and this backend does not
 * re-enable it. Inheriting the container's setting would mean a misspelled field in a Wiring row
 * binding the declared type's default and nobody hearing about it — the exact failure that reading
 * configuration at compile time exists to prevent. Wiring configuration is configuration, not user
 * input, so an unrecognised field is a broken definition rather than something to tolerate.
 *
 * <p>Whether a row may carry configuration at all is decided from the declared params type and the
 * raw blob before either reaches Jackson.
 *
 * <p>Every refusal is an {@link InvalidGraphException} raised when a definition is compiled, never
 * when an Event fires, so a Wiring row with a typo in it fails once where the administrator
 * publishing it can see it.
 */
final class WiringConfigurationReader {

  /** A JSON {@code null} blob says the same thing an absent column does. */
  private static final String JSON_NULL = "null";

  /**
   * And an empty object is an ordinary way for a row to say it configures nothing. Accepted only
   * for a strategy that takes no params; for one that does take some, it is a present object and is
   * read as such.
   */
  private static final String EMPTY_JSON_OBJECT = "{}";

  private final String strategyNoun;
  private final ObjectReader reader;

  /** Reads the Guard Wiring table's configuration, and says "Guard" when it refuses a row. */
  static WiringConfigurationReader forGuards(ObjectMapper objectMapper) {
    return new WiringConfigurationReader("Guard", objectMapper);
  }

  /** The same over the Action Wiring table, so a refusal names the rows to go and look at. */
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
   *     declared type — an unrecognised field included
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
   * The spellings of an unconfigured row that carry no object at all. {@code params} is {@code
   * JSONB} mapped to a Java {@code String}, so an absent column arrives as {@code null} and a JSON
   * {@code null} arrives as the four characters that spell it.
   */
  private static boolean carriesNothing(String paramsJson) {
    return paramsJson == null || paramsJson.isBlank() || JSON_NULL.equals(paramsJson.strip());
  }
}
