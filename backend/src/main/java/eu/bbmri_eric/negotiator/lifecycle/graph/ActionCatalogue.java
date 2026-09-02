package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * The Action-side twin of {@link GuardCatalogue}: given an Action Wiring row's type key and its raw
 * jsonb params, hand back something callable.
 *
 * <p>Two catalogues rather than one, for the same reason ADR 0002 gives two wiring tables. Guards
 * and Actions answer different questions and are keyed in separate namespaces; a single registry
 * over one shared string space would let an Action be wired where a Guard belongs and have that
 * discovered at fire time.
 */
public interface ActionCatalogue {

  /**
   * Binds one Action Wiring row to its strategy.
   *
   * @param typeKey the row's {@code type_key}
   * @param paramsJson the row's {@code params}, or null when the strategy takes none
   * @throws IllegalArgumentException if no strategy declares that key, or if the params do not read
   *     into the strategy's declared type
   */
  ActionStep bind(String typeKey, String paramsJson);
}
