package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * Turns a Guard Wiring row's type key and jsonb params into a callable {@link GuardStep}.
 *
 * <p>Guard logic stays in Java and only the Wiring is data. This interface keeps the definition
 * package, which knows rows, apart from the evaluation package, which knows strategy beans and
 * Jackson. A bad type key or params fail here at compile time, not when a user fires an Event.
 */
public interface GuardCatalogue {

  /**
   * Binds one Guard Wiring row to its strategy.
   *
   * @param typeKey the row's {@code type_key}
   * @param paramsJson the row's {@code params}, or null — which is legal and ordinary, since a
   *     strategy that takes no parameters needs none
   * @throws IllegalArgumentException if no strategy declares that key, or if the params do not read
   *     into the strategy's declared type
   */
  GuardStep bind(String typeKey, String paramsJson);
}
