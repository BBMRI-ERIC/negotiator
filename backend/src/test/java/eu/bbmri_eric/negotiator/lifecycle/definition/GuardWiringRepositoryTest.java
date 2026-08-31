package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.OTHER_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.definitionIn;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.eventIn;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.stateIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.lifecycle.RequiredAuthority;
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

  /**
   * A second Transition in the <em>same</em> Definition Version as {@link #transition}, for the
   * scope tests. {@link #transitionInOtherDefinition} is the cross-definition one.
   */
  private Transition secondTransition;

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
    secondTransition =
        transitions.saveAndFlush(
            Transition.builder()
                .lifecycleDefinition(definition)
                .fromState(submitted)
                .toState(draft)
                .event(abandon)
                .requiredAuthority(RequiredAuthority.NONE)
                .build());
    otherDefinition = definitions.saveAndFlush(definitionIn(OTHER_FAMILY));
    State otherDraft = states.saveAndFlush(stateIn(otherDefinition, "DRAFT"));
    State otherSubmitted = states.saveAndFlush(stateIn(otherDefinition, "SUBMITTED"));
    Event otherSubmit = events.saveAndFlush(eventIn(otherDefinition, "SUBMIT"));
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
    assertEquals(definition.getId(), found.getLifecycleDefinition().getId());
    assertEquals(transition.getId(), found.getTransition().getId());
    assertEquals("REQUIREMENT_MET", found.getTypeKey());
    assertEquals(1, found.getSortOrder());
  }

  /**
   * Null {@code transition_id} means the Guard applies to every Transition of the definition. This
   * is also the case PostgreSQL's default {@code MATCH SIMPLE} leaves unconstrained: the composite
   * foreign key on {@code (lifecycle_definition_id, transition_id)} is skipped entirely when either
   * column is null, so the shape has to be shown accepted rather than assumed.
   */
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
   * A non-trivial JSON payload survives the round trip through a jsonb column. The payload goes in
   * deliberately <em>non-canonical</em> — keys out of order, no spaces after the separators — and
   * is read back through {@link JdbcTemplate} so the assertion is against the stored column value
   * rather than against whatever Hibernate chose to hand back.
   *
   * <p>Only jsonb rewrites it. jsonb normalises object keys to length-then-bytewise order (hence
   * {@code mode}, {@code within}, {@code requirementIds}) and re-spaces the separators, while
   * leaving array element order alone. A {@code text} or {@code json} column would return the input
   * byte-for-byte, so the reordering is what pins this column to jsonb.
   */
  @Test
  void save_withParams_roundTripsNonTrivialJsonPayload() {
    String payload = "{\"mode\":\"ALL\",\"within\":{\"days\":14},\"requirementIds\":[7,3]}";
    GuardWiring guard =
        guardWirings.saveAndFlush(
            GuardWiring.builder()
                .lifecycleDefinition(definition)
                .typeKey("REQUIREMENT_MET")
                .params(payload)
                .sortOrder(1)
                .build());

    String stored =
        jdbcTemplate.queryForObject(
            "SELECT params::text FROM guard_wiring WHERE id = ?", String.class, guard.getId());
    assertEquals(
        "{\"mode\": \"ALL\", \"within\": {\"days\": 14}, \"requirementIds\": [7, 3]}", stored);
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
   *
   * <p>This is the test that rules out a single non-partial index on {@code (definition,
   * transition, sort_order)}: PostgreSQL treats nulls as distinct in a plain unique index, so that
   * shape would accept both of these rows while still passing every other test in this class.
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

    GuardWiring elsewhere = transitionScopedGuard(secondTransition, "GUARD_B", 1);
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

  /**
   * {@code lifecycle_definition_id} is NOT NULL: every Guard belongs to a Definition Version, even
   * the definition-scoped ones. Inserted through {@link JdbcTemplate} because the v1 seed is SQL
   * that bypasses the mapping, so Hibernate's {@code nullable = false} would otherwise mask whether
   * the column itself carries the constraint.
   */
  @Test
  void insert_withoutADefinition_isRefusedByTheDatabase() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO guard_wiring (lifecycle_definition_id, type_key, sort_order)"
                    + " VALUES (NULL, 'REQUIREMENT_MET', 1)"));
  }

  /**
   * For a definition-scoped Guard the composite foreign key is skipped — {@code MATCH SIMPLE} stops
   * checking as soon as {@code transition_id} is null — so {@code
   * fk_guard_wiring_lifecycle_definition} is the only thing left tying the row to a real Definition
   * Version. Unlike slice 03's redundant definition FK, this one is not transitively implied by any
   * other constraint, so it can fail and is worth its own test.
   */
  @Test
  void insert_withADefinitionThatDoesNotExist_isRefusedByTheDatabase() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO guard_wiring (lifecycle_definition_id, type_key, sort_order)"
                    + " VALUES (-1, 'REQUIREMENT_MET', 1)"));
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
}
