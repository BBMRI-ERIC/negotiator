package eu.bbmri_eric.negotiator.lifecycle.definition;

/**
 * The Definition Version, State and Event rows that several tests in this package need before they
 * reach the table under test, plus two catalogue fakes for tests that compile rows without caring
 * what a strategy does. A helper belongs here only if more than one test class uses it. {@link
 * #stateBuilder} has one calling class but stays here because {@link #stateIn} builds on it.
 *
 * <p>Every helper builds the minimum valid row for its table, with what the DDL demands and nothing
 * else, so a test asserting on a column asserts on a value it set itself. A helper that set an
 * optional column would change what several refusal tests refuse.
 *
 * <p>Every helper builds the <em>minimum valid</em> row for its table: whatever the DDL demands and
 * nothing else, so a test asserting on a column is asserting on a value it set itself. One that
 * grew an optional column would quietly change what several refusal tests are refusing.
 *
 * <p>Package private, and beside the tests rather than under {@code integration/repository/}, for
 * the same reason the tests are: the entities it builds are package private.
 */
final class DefinitionFixtures {

  /** The Definition Family of the v1 seed, used unless a test needs two families. */
  static final String STANDARD_FAMILY = "standard-negotiation-flow";

  /**
   * A second family, for tests that check a constraint is scoped to one Definition Version. A row
   * in this family must be accepted because it sits in a different definition.
   */
  static final String OTHER_FAMILY = "expedited-negotiation-flow";

  private DefinitionFixtures() {}

  /** Version 1 of {@code familyKey}, named after it, neither active nor the global default. */
  static LifecycleDefinition definitionIn(String familyKey) {
    return LifecycleDefinition.builder()
        .scope(DefinitionScope.NEGOTIATION)
        .familyKey(familyKey)
        .name(familyKey)
        .version(1)
        .build();
  }

  /** A State named and labelled {@code name}, neither initial nor terminal. */
  static State stateIn(LifecycleDefinition definition, String name) {
    return stateBuilder(definition, name).build();
  }

  /** {@link #stateIn} without its {@code build()}, for tests that set the initial or terminal flag. */
  static State.StateBuilder stateBuilder(LifecycleDefinition definition, String name) {
    return State.builder().lifecycleDefinition(definition).name(name).label(name);
  }

  /** An Event named {@code name}. */
  static Event eventIn(LifecycleDefinition definition, String name) {
    return Event.builder().lifecycleDefinition(definition).name(name).build();
  }
}
