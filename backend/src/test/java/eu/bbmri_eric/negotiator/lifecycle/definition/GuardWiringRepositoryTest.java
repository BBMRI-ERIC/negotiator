package eu.bbmri_eric.negotiator.lifecycle.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Persistence behaviour of Guard wiring — the configuration attaching a Guard to where it applies.
 * A Guard may apply to one Transition, or to an entire Definition Version and therefore all of its
 * Transitions; the latter is spelled as a null {@code transition_id}.
 *
 * <p>The frozen graph dump carries 21 of 21 Transitions with no Guard, so no v1 data will exercise
 * this table. These tests are the only evidence the table works, so the refusals and the two-scope
 * acceptance are all load-bearing rather than hygiene.
 */
@RepositoryTest
class GuardWiringRepositoryTest {

  private static final String STANDARD_FAMILY = "standard-negotiation-flow";
  private static final String OTHER_FAMILY = "expedited-negotiation-flow";

  @Autowired GuardWiringRepository guardWirings;
  @Autowired TransitionRepository transitions;
  @Autowired StateRepository states;
  @Autowired EventRepository events;
  @Autowired LifecycleDefinitionRepository definitions;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbcTemplate;

  private LifecycleDefinition definition;
  private LifecycleDefinition otherDefinition;
  private Transition transition;
  private Transition otherTransition;
  private Transition transitionInOtherDefinition;

  @BeforeEach
  void setUp() {
    definition = definitions.saveAndFlush(definitionIn(STANDARD_FAMILY));
    State draft = states.saveAndFlush(stateIn(definition, "DRAFT"));
    State submitted = states.saveAndFlush(stateIn(definition, "SUBMITTED"));
    Event submit = events.saveAndFlush(eventIn(definition, "SUBMIT"));
    Event abandon = events.saveAndFlush(eventIn(definition, "ABANDON"));
    transition =
        transitions.saveAndFlush(
            Transition.builder()
                .lifecycleDefinition(definition)
                .fromState(draft)
                .toState(submitted)
                .event(submit)
                .requiredAuthority(RequiredAuthority.NONE)
                .build());
    otherDefinition = definitions.saveAndFlush(definitionIn(OTHER_FAMILY));
    State otherDraft = states.saveAndFlush(stateIn(otherDefinition, "DRAFT"));
    State otherSubmitted = states.saveAndFlush(stateIn(otherDefinition, "SUBMITTED"));
    Event otherSubmit = events.saveAndFlush(eventIn(otherDefinition, "SUBMIT"));
    otherTransition =
        transitions.saveAndFlush(
            Transition.builder()
                .lifecycleDefinition(definition)
                .fromState(submitted)
                .toState(draft)
                .event(abandon)
                .requiredAuthority(RequiredAuthority.NONE)
                .build());
    transitionInOtherDefinition =
        transitions.saveAndFlush(
            Transition.builder()
                .lifecycleDefinition(otherDefinition)
                .fromState(otherDraft)
                .toState(otherSubmitted)
                .event(otherSubmit)
                .requiredAuthority(RequiredAuthority.NONE)
                .build());
  }

  /** A Guard scoped to one Transition round-trips with its reference intact. */
  @Test
  void save_transitionScoped_roundTrips() {
    GuardWiring guard =
        guardWirings.saveAndFlush(
            GuardWiring.builder()
                .lifecycleDefinition(definition)
                .transition(transition)
                .typeKey("REQUIREMENT_MET")
                .params(null)
                .sortOrder(1)
                .build());
    entityManager.clear();

    GuardWiring found = guardWirings.findById(guard.getId()).orElseThrow();
    assertNotNull(found.getId());
    assertEquals(transition.getId(), found.getTransition().getId());
    assertEquals("REQUIREMENT_MET", found.getTypeKey());
    assertEquals(1, found.getSortOrder());
  }

  /** Null {@code transition_id} means the Guard applies to every Transition of the definition. */
  @Test
  void save_definitionScoped_roundTrips() {
    GuardWiring guard =
        guardWirings.saveAndFlush(
            GuardWiring.builder()
                .lifecycleDefinition(definition)
                .transition(null)
                .typeKey("NEGOTIATION_APPROVED")
                .params(null)
                .sortOrder(1)
                .build());
    entityManager.clear();

    GuardWiring found = guardWirings.findById(guard.getId()).orElseThrow();
    assertNotNull(found.getId());
    assertEquals(definition.getId(), found.getLifecycleDefinition().getId());
    assertNull(found.getTransition());
    assertEquals("NEGOTIATION_APPROVED", found.getTypeKey());
    assertEquals(1, found.getSortOrder());
  }

  /**
   * A non-trivial JSON payload survives the round trip through a jsonb column. Read back through
   * {@link JdbcTemplate} because that reads the column value directly — proving the jsonb column
   * holds what was written, not just that Hibernate can return a string. The payload is written in
   * PostgreSQL's canonical jsonb text form (sorted keys, space after each separator) so the
   * comparison is byte-for-byte, since jsonb normalises any other input on storage.
   */
  @Test
  void save_withParams_roundTripsNonTrivialJsonPayload() {
    String payload = "{\"flag\": true, \"scope\": \"POST\"}";
    GuardWiring guard =
        guardWirings.saveAndFlush(
            GuardWiring.builder()
                .lifecycleDefinition(definition)
                .typeKey("SET_POST_VISIBILITY")
                .params(payload)
                .sortOrder(1)
                .build());

    String stored =
        jdbcTemplate.queryForObject(
            "SELECT params::text FROM guard_wiring WHERE id = ?", String.class, guard.getId());
    assertEquals(payload, stored);
  }

  /** A strategy that takes no parameters needs no params row — null is accepted. */
  @Test
  void save_withNullParams_isAccepted() {
    GuardWiring guard =
        guardWirings.saveAndFlush(
            GuardWiring.builder()
                .lifecycleDefinition(definition)
                .typeKey("REQUIREMENT_MET")
                .params(null)
                .sortOrder(1)
                .build());
    entityManager.clear();

    GuardWiring found = guardWirings.findById(guard.getId()).orElseThrow();
    assertNull(found.getParams());
  }

  /**
   * Two definition-scoped Guards with the same {@code sort_order} in the same definition would make
   * the effective chain non-deterministic. The partial unique index on {@code (definition,
   * sort_order) WHERE transition_id IS NULL} refuses the second.
   */
  @Test
  void save_withDuplicateSortOrderInDefinitionScope_isRefused() {
    guardWirings.saveAndFlush(definitionScopedGuard(definition, "GUARD_A", 1));

    GuardWiring duplicate = definitionScopedGuard(definition, "GUARD_B", 1);
    DataIntegrityViolationException refused =
        assertThrows(
            DataIntegrityViolationException.class, () -> guardWirings.saveAndFlush(duplicate));
    assertTrue(refused.getMessage().contains("uq_guard_wiring_sort_order_definition"));
  }

  /**
   * Two transition-scoped Guards with the same {@code sort_order} on the same Transition are the
   * same ambiguity within that Transition's chain. The partial unique index on {@code (transition,
   * sort_order) WHERE transition_id IS NOT NULL} refuses the second.
   */
  @Test
  void save_withDuplicateSortOrderInTransitionScope_isRefused() {
    guardWirings.saveAndFlush(transitionScopedGuard(transition, "GUARD_A", 1));

    GuardWiring duplicate = transitionScopedGuard(transition, "GUARD_B", 1);
    DataIntegrityViolationException refused =
        assertThrows(
            DataIntegrityViolationException.class, () -> guardWirings.saveAndFlush(duplicate));
    assertTrue(refused.getMessage().contains("uq_guard_wiring_sort_order_transition"));
  }

  /**
   * The two scopes have independent {@code sort_order} sequences: a definition-scoped Guard at
   * {@code sort_order = 1} does not conflict with a transition-scoped Guard at {@code sort_order =
   * 1} in the same definition. This is the test that proves the two partial indexes do not overlap.
   */
  @Test
  void save_withTheSameSortOrderInDifferentScopes_isAccepted() {
    guardWirings.saveAndFlush(definitionScopedGuard(definition, "GUARD_A", 1));

    GuardWiring transitionScoped = transitionScopedGuard(transition, "GUARD_B", 1);
    assertNotNull(guardWirings.saveAndFlush(transitionScoped).getId());
  }

  /** The definition-scoped index includes the definition, so the same order is fine elsewhere. */
  @Test
  void save_withTheSameSortOrderInAnotherDefinition_isAccepted() {
    guardWirings.saveAndFlush(definitionScopedGuard(definition, "GUARD_A", 1));

    GuardWiring elsewhere = definitionScopedGuard(otherDefinition, "GUARD_B", 1);
    assertNotNull(guardWirings.saveAndFlush(elsewhere).getId());
  }

  /** The transition-scoped index includes the transition, so the same order is fine on another. */
  @Test
  void save_withTheSameSortOrderOnAnotherTransition_isAccepted() {
    guardWirings.saveAndFlush(transitionScopedGuard(transition, "GUARD_A", 1));

    GuardWiring elsewhere = transitionScopedGuard(otherTransition, "GUARD_B", 1);
    assertNotNull(guardWirings.saveAndFlush(elsewhere).getId());
  }

  /**
   * A Guard must not reference a Transition from a different Lifecycle Definition — loading a
   * definition's Guards would otherwise return a row pointing out of its graph. The composite FK on
   * {@code (lifecycle_definition_id, transition_id)} refuses the straddle, the same pattern slice
   * 03 applied to Transition's own state and event references.
   */
  @Test
  void save_withATransitionFromAnotherDefinition_isRefused() {
    GuardWiring straddling =
        GuardWiring.builder()
            .lifecycleDefinition(definition)
            .transition(transitionInOtherDefinition)
            .typeKey("REQUIREMENT_MET")
            .sortOrder(1)
            .build();
    assertThrows(
        DataIntegrityViolationException.class, () -> guardWirings.saveAndFlush(straddling));
  }

  private static GuardWiring definitionScopedGuard(
      LifecycleDefinition def, String typeKey, int sortOrder) {
    return GuardWiring.builder()
        .lifecycleDefinition(def)
        .transition(null)
        .typeKey(typeKey)
        .sortOrder(sortOrder)
        .build();
  }

  private static GuardWiring transitionScopedGuard(
      Transition transition, String typeKey, int sortOrder) {
    return GuardWiring.builder()
        .lifecycleDefinition(transition.getLifecycleDefinition())
        .transition(transition)
        .typeKey(typeKey)
        .sortOrder(sortOrder)
        .build();
  }

  private static State stateIn(LifecycleDefinition definition, String name) {
    return State.builder().lifecycleDefinition(definition).name(name).label(name).build();
  }

  private static Event eventIn(LifecycleDefinition definition, String name) {
    return Event.builder().lifecycleDefinition(definition).name(name).build();
  }

  private static LifecycleDefinition definitionIn(String familyKey) {
    return LifecycleDefinition.builder()
        .scope(DefinitionScope.NEGOTIATION)
        .familyKey(familyKey)
        .name(familyKey)
        .version(1)
        .build();
  }
}
