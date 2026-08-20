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
 * Persistence behaviour of Guard Wiring, the configuration that attaches a Guard to where it
 * applies. A Guard applies to one Transition, or to a whole Definition Version and so to all of its
 * Transitions. A null {@code transition_id} marks the second case.
 *
 * <p>None of the 21 Transitions in the v1 seed has a Guard, so v1 data never exercises this table.
 * These tests are the only evidence that it works.
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

  /**
   * A second Transition in the same Definition Version as {@link #transition}, for the scope tests.
   * {@link #transitionInOtherDefinition} is the one in another definition.
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
   * A null {@code transition_id} means the Guard applies to every Transition of the definition.
   * PostgreSQL's default {@code MATCH SIMPLE} skips the composite foreign key on {@code
   * (lifecycle_definition_id, transition_id)} when either column is null, so the test shows the row
   * is accepted instead of assuming it.
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
   * A nested JSON payload survives the round trip through a jsonb column. The payload goes in
   * non-canonical, with keys out of order and no spaces after the separators. The test reads it
   * back through {@link JdbcTemplate} to assert on the stored column value, not on what Hibernate
   * returns.
   *
   * <p>jsonb sorts object keys by length, then bytewise ({@code mode}, {@code within}, {@code
   * requirementIds}), re-spaces the separators, and keeps array order. A {@code text} or {@code
   * json} column would return the input unchanged, so the reordering proves the column is jsonb.
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

  /** A Guard that takes no parameters stores null params. */
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
   * Two definition-scoped Guards with the same {@code sort_order} in the same definition would run
   * in an undefined order. The partial unique index on {@code (definition, sort_order) WHERE
   * transition_id IS NULL} refuses the second.
   *
   * <p>This test rules out a single plain index on {@code (definition, transition, sort_order)}.
   * PostgreSQL treats nulls as distinct in a plain unique index, so it would accept both rows and
   * still pass every other test in this class.
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
   * Two transition-scoped Guards with the same {@code sort_order} on the same Transition have the
   * same problem. The partial unique index on {@code (transition, sort_order) WHERE transition_id IS
   * NOT NULL} refuses the second.
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
   * The two scopes have independent {@code sort_order} sequences, so a definition-scoped Guard and a
   * transition-scoped Guard may both use {@code sort_order = 1} in the same definition. This proves
   * the two partial indexes do not overlap.
   */
  @Test
  void save_withTheSameSortOrderInDifferentScopes_isAccepted() {
    guardWirings.saveAndFlush(definitionScopedGuard(definition, "GUARD_A", 1));

    GuardWiring transitionScoped = transitionScopedGuard(transition, "GUARD_B", 1);
    assertNotNull(guardWirings.saveAndFlush(transitionScoped).getId());
  }

  /** The definition-scoped index includes the definition, so another definition may reuse it. */
  @Test
  void save_withTheSameSortOrderInAnotherDefinition_isAccepted() {
    guardWirings.saveAndFlush(definitionScopedGuard(definition, "GUARD_A", 1));

    GuardWiring elsewhere = definitionScopedGuard(otherDefinition, "GUARD_B", 1);
    assertNotNull(guardWirings.saveAndFlush(elsewhere).getId());
  }

  /** The transition-scoped index includes the Transition, so another Transition may reuse it. */
  @Test
  void save_withTheSameSortOrderOnAnotherTransition_isAccepted() {
    guardWirings.saveAndFlush(transitionScopedGuard(transition, "GUARD_A", 1));

    GuardWiring elsewhere = transitionScopedGuard(secondTransition, "GUARD_B", 1);
    assertNotNull(guardWirings.saveAndFlush(elsewhere).getId());
  }

  /**
   * A Guard must not reference a Transition from another Definition Version, or loading a
   * definition's Guards would return a row pointing outside its graph. The composite foreign key on
   * {@code (lifecycle_definition_id, transition_id)} refuses it, as the Transition table's own keys
   * do for its State and Event references.
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
   * {@code lifecycle_definition_id} is NOT NULL because every Guard belongs to a Definition Version.
   * The test inserts through {@link JdbcTemplate} because the v1 seed is SQL that bypasses the
   * mapping, and Hibernate's {@code nullable = false} would hide whether the column itself has the
   * constraint.
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
   * {@code MATCH SIMPLE} skips the composite foreign key when {@code transition_id} is null, so for
   * a definition-scoped Guard only {@code fk_guard_wiring_lifecycle_definition} ties the row to a
   * real Definition Version. No other constraint implies it, so it gets its own test.
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
