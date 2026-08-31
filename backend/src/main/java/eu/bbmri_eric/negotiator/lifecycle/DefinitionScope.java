package eu.bbmri_eric.negotiator.lifecycle;

/**
 * Which kind of Lifecycle a Definition Family governs: a Negotiation's, or a Resource's within a
 * Negotiation. Fixed for the whole family.
 *
 * <p>It lives here beside the Well-known name holders rather than with the entity that stores it,
 * because it is vocabulary and not schema: a closed set of two names that no definition author
 * extends, naming a distinction in the domain rather than a shape in a table. Its persistence is an
 * {@code @Enumerated(STRING)} column on the Definition Version entity, and that annotation stays
 * with the entity - nothing about this type knows a table exists.
 *
 * <p>Living up here is what lets a component name the distinction without reaching the definition
 * package, which is the whole point of the move: the components that need this vocabulary are the
 * ones that must stay structurally unable to query the definition tables.
 */
public enum DefinitionScope {
  NEGOTIATION,
  RESOURCE
}
