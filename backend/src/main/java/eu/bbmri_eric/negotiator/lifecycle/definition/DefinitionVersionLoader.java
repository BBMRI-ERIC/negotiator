package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads one Definition Version's six tables into {@link DefinitionVersionRows}, one query per
 * table. A single join across all six would multiply every State by every Wiring row.
 *
 * <p>All six queries must share one transaction. {@link DefinitionCompiler} compares rows by
 * reference, and six persistence contexts would give it six instances of the same row, so it would
 * refuse a valid version as mixing two. A {@code @DataJpaTest} cannot catch this because it wraps
 * each test in a transaction, so an integration test that commits covers it.
 *
 * <p>It reads the Definition Version first, so every {@code lifecycle_definition_id} below resolves
 * to that instance rather than to a new proxy.
 *
 * <p>The fetch joins initialize everything the compiler reads. Compilation runs after this
 * transaction commits, so a lazy association would throw {@code LazyInitializationException}.
 */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DefinitionVersionLoader {

  private final LifecycleDefinitionRepository definitions;
  private final StateRepository states;
  private final EventRepository events;
  private final TransitionRepository transitions;
  private final GuardWiringRepository guardWirings;
  private final ActionWiringRepository actionWirings;

  /**
   * Every row of the named Definition Version.
   *
   * <p>The method is public because Spring's proxy-based {@code @Transactional} ignores
   * package-private methods.
   *
   * @throws InvalidGraphException if no Definition Version has that id, which means a Definition
   *     Version Pin names a missing row
   */
  @Transactional(readOnly = true)
  public DefinitionVersionRows load(long definitionVersionId) {
    LifecycleDefinition definition =
        definitions
            .findById(definitionVersionId)
            .orElseThrow(
                () ->
                    new InvalidGraphException(
                        ("No Lifecycle Definition has id %d, so no graph can be compiled for it. "
                                + "Work pinned to that Definition Version cannot be judged at all.")
                            .formatted(definitionVersionId)));
    return new DefinitionVersionRows(
        definition,
        states.findByLifecycleDefinitionIdOrderById(definitionVersionId),
        events.findByLifecycleDefinitionIdOrderById(definitionVersionId),
        transitions.findByLifecycleDefinitionIdOrderById(definitionVersionId),
        guardWirings.findByLifecycleDefinitionIdOrderById(definitionVersionId),
        actionWirings.findByTransitionLifecycleDefinitionIdOrderById(definitionVersionId));
  }
}
