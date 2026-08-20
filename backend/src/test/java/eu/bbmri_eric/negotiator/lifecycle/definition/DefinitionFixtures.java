package eu.bbmri_eric.negotiator.lifecycle.definition;

/**
 * The Definition Version, State and Event rows that more than one repository test in this package
 * needs before it can get to the table it is actually testing. Five of the six test classes were
 * carrying byte-identical copies of these — {@code definitionIn} in five, {@code eventIn} in four,
 * {@code stateIn} in three — and the sixth carried the two family constants.
 *
 * <p>Being needed by more than one test class is the whole criterion for living here. {@code
 * GuardWiringRepositoryTest}'s two wiring builders and {@code LifecycleDefinitionRepositoryTest}'s
 * {@code versionBuilder} have one test class each and stay with it. {@code versionBuilder}
 * additionally must never be folded into {@link #definitionIn}, which it resembles: it names the
 * definition {@code "Standard flow"} rather than after its family key, and takes the version as a
 * parameter, because varying the version within one family is that test's subject. {@link
 * #stateBuilder} is the one member here with a single calling class, and it is here because {@link
 * #stateIn} is defined in terms of it — leaving it behind would put the same builder chain in two
 * files, which is what this class exists to stop.
 *
 * <p>Every helper builds the <em>minimum valid</em> row for its table: whatever the DDL demands and
 * nothing else, so a test asserting on a column is asserting on a value it set itself. One that
 * grew an optional column would quietly change what several refusal tests are refusing.
 *
 * <p>Package private, and beside the tests rather than under {@code integration/repository/}, for
 * the same reason the tests are: the entities it builds are package private.
 */
final class DefinitionFixtures {

  /** The family the frozen v1 graph belongs to; the subject unless a test needs two. */
  static final String STANDARD_FAMILY = "standard-negotiation-flow";

  /**
   * A second family, for every test whose subject is that a constraint is scoped to one Definition
   * Version — the row that must be <em>accepted</em> because it sits in a different definition.
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

  /**
   * {@link #stateIn} short of its {@code build()}, for the tests whose subject is one of the two
   * flags it leaves at their defaults.
   */
  static State.StateBuilder stateBuilder(LifecycleDefinition definition, String name) {
    return State.builder().lifecycleDefinition(definition).name(name).label(name);
  }

  /** An Event named {@code name}. The name is the whole row besides its owner. */
  static Event eventIn(LifecycleDefinition definition, String name) {
    return Event.builder().lifecycleDefinition(definition).name(name).build();
  }
}
