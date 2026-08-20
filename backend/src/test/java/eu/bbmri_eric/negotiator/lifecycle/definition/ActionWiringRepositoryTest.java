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
 * Persistence behaviour of Action wiring — the configuration attaching an Action to the Transition
 * it runs after. Action wiring is always transition-scoped and carries no definition reference at
 * all, since the Transition already implies it.
 *
 * <p>Only three Transitions in the frozen graph dump carry Actions, and each carries exactly one,
 * so sort_order uniqueness within a Transition is unexercised by v1 data. The refusal test is the
 * only evidence it works.
 */
@RepositoryTest
class ActionWiringRepositoryTest {

  private static final String STANDARD_FAMILY = "standard-negotiation-flow";

  @Autowired ActionWiringRepository actionWirings;
  @Autowired TransitionRepository transitions;
  @Autowired StateRepository states;
  @Autowired EventRepository events;
  @Autowired LifecycleDefinitionRepository definitions;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbcTemplate;

  private Transition transition;

  @BeforeEach
  void setUp() {
    LifecycleDefinition definition = definitions.saveAndFlush(definitionIn(STANDARD_FAMILY));
    State draft = states.saveAndFlush(stateIn(definition, "DRAFT"));
    State submitted = states.saveAndFlush(stateIn(definition, "SUBMITTED"));
    Event submit = events.saveAndFlush(eventIn(definition, "SUBMIT"));
    transition =
        transitions.saveAndFlush(
            Transition.builder()
                .lifecycleDefinition(definition)
                .fromState(draft)
                .toState(submitted)
                .event(submit)
                .requiredAuthority(RequiredAuthority.NONE)
                .build());
  }

  @Test
  void save_withEveryColumnPopulated_roundTrips() {
    ActionWiring action =
        actionWirings.saveAndFlush(
            ActionWiring.builder()
                .transition(transition)
                .typeKey("SET_POST_VISIBILITY")
                .params("{\"flag\": true, \"scope\": \"POST\"}")
                .sortOrder(1)
                .build());
    entityManager.clear();

    ActionWiring found = actionWirings.findById(action.getId()).orElseThrow();
    assertNotNull(found.getId());
    assertEquals(transition.getId(), found.getTransition().getId());
    assertEquals("SET_POST_VISIBILITY", found.getTypeKey());
    assertEquals(1, found.getSortOrder());
  }

  /**
   * A non-trivial JSON payload survives the round trip through a jsonb column. The payload goes in
   * deliberately <em>non-canonical</em> — keys out of order, no spaces after the separators — and
   * is read back through {@link JdbcTemplate} so the assertion is against the stored column value
   * rather than against whatever Hibernate chose to hand back.
   *
   * <p>Only jsonb rewrites it. jsonb normalises object keys to length-then-bytewise order (hence
   * {@code flag}, {@code scope}, {@code appliesTo}) and re-spaces the separators, while leaving
   * array element order alone. A {@code text} or {@code json} column would return the input
   * byte-for-byte, so the reordering is what pins this column to jsonb.
   */
  @Test
  void save_withParams_roundTripsNonTrivialJsonPayload() {
    String payload =
        "{\"scope\":\"POST\",\"flag\":false,\"appliesTo\":{\"roles\":[2,1],\"all\":true}}";
    ActionWiring action =
        actionWirings.saveAndFlush(
            ActionWiring.builder()
                .transition(transition)
                .typeKey("SET_POST_VISIBILITY")
                .params(payload)
                .sortOrder(1)
                .build());

    String stored =
        jdbcTemplate.queryForObject(
            "SELECT params::text FROM action_wiring WHERE id = ?", String.class, action.getId());
    assertEquals(
        "{\"flag\": false, \"scope\": \"POST\", \"appliesTo\": {\"all\": true, \"roles\": [2, 1]}}",
        stored);
  }

  /** A strategy that takes no parameters needs no params row — null is accepted. */
  @Test
  void save_withNullParams_isAccepted() {
    ActionWiring action =
        actionWirings.saveAndFlush(
            ActionWiring.builder()
                .transition(transition)
                .typeKey("SPAWN_RESOURCE_LIFECYCLES")
                .params(null)
                .sortOrder(1)
                .build());
    entityManager.clear();

    ActionWiring found = actionWirings.findById(action.getId()).orElseThrow();
    assertNull(found.getParams());
  }

  /**
   * Two Actions with the same {@code sort_order} on the same Transition would make the chain
   * non-deterministic. Inserted through the repository to prove the constraint fires on a mapped
   * save, not only on raw SQL.
   *
   * <p>Both rows carry the same {@code type_key} and differ only in {@code params} — neither of
   * which is a constrained column — so {@code sort_order} is the only thing the refusal can be
   * about. That pair is also the collision the collapse of the three post-visibility Action classes
   * actually creates: one typed key, two configurations, one Transition.
   */
  @Test
  void save_withDuplicateSortOrderOnTheSameTransition_isRefused() {
    actionWirings.saveAndFlush(
        ActionWiring.builder()
            .transition(transition)
            .typeKey("SET_POST_VISIBILITY")
            .params("{\"flag\": true, \"scope\": \"POST\"}")
            .sortOrder(1)
            .build());

    ActionWiring duplicate =
        ActionWiring.builder()
            .transition(transition)
            .typeKey("SET_POST_VISIBILITY")
            .params("{\"flag\": false, \"scope\": \"POST\"}")
            .sortOrder(1)
            .build();
    DataIntegrityViolationException refused =
        assertThrows(
            DataIntegrityViolationException.class, () -> actionWirings.saveAndFlush(duplicate));
    assertTrue(refused.getMessage().contains("uq_action_wiring_transition_sort_order"));
  }

  /** The index includes the transition, so the same order is fine on another. */
  @Test
  void save_withTheSameSortOrderOnAnotherTransition_isAccepted() {
    actionWirings.saveAndFlush(
        ActionWiring.builder()
            .transition(transition)
            .typeKey("SET_POST_VISIBILITY")
            .sortOrder(1)
            .build());

    Transition another =
        transitions.saveAndFlush(
            Transition.builder()
                .lifecycleDefinition(transition.getLifecycleDefinition())
                .fromState(transition.getToState())
                .toState(transition.getFromState())
                .event(events.saveAndFlush(eventIn(transition.getLifecycleDefinition(), "ABANDON")))
                .requiredAuthority(RequiredAuthority.NONE)
                .build());
    ActionWiring elsewhere =
        ActionWiring.builder()
            .transition(another)
            .typeKey("SET_POST_VISIBILITY")
            .params("{\"flag\": false, \"scope\": \"POST\"}")
            .sortOrder(1)
            .build();
    assertNotNull(actionWirings.saveAndFlush(elsewhere).getId());
  }

  /**
   * {@code transition_id} is NOT NULL: Actions are always transition-scoped. Inserted through
   * {@link JdbcTemplate} because the v1 seed is SQL that bypasses the mapping, so Hibernate's
   * {@code nullable = false} would otherwise mask whether the column itself carries the constraint.
   */
  @Test
  void insert_withoutATransition_isRefusedByTheDatabase() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO action_wiring (transition_id, type_key, sort_order)"
                    + " VALUES (NULL, 'SET_POST_VISIBILITY', 1)"));
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
