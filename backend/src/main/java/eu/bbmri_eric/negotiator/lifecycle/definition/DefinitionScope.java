package eu.bbmri_eric.negotiator.lifecycle.definition;

/**
 * Which kind of Lifecycle a Definition Family governs: a Negotiation's, or a Resource's within a
 * Negotiation. Fixed for the whole family.
 */
enum DefinitionScope {
  NEGOTIATION,
  RESOURCE
}
