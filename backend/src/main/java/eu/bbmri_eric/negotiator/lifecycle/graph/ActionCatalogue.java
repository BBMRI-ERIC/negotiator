package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * Turns an Action Wiring row's type key and raw jsonb params into a callable {@link ActionStep}.
 *
 * <p>It is separate from {@link GuardCatalogue} so that a Wiring cannot name an Action where a Guard
 * belongs.
 */
public interface ActionCatalogue {

  /**
   * Binds one Action Wiring row to its strategy.
   *
   * @param typeKey the row's {@code type_key}
   * @param paramsJson the row's {@code params}, or null when the strategy takes none
   * @throws InvalidGraphException if no strategy declares that key, or if the params do not read
   *     into the strategy's declared type
   */
  ActionStep bind(String typeKey, String paramsJson);
}
