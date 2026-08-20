package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.definitionIn;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.eventIn;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.stateIn;
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
 * Persistence behaviour of Action Wiring, the configuration that attaches an Action to the
 * Transition it runs after. Action Wiring is always transition-scoped and has no definition
 * reference, because the Transition implies it.
 *
 * <p>Only three Transitions in the v1 seed have Actions, one each, so v1 data never exercises
 * sort_order uniqueness within a Transition. The refusal test is the only evidence that it works.
 */
@RepositoryTest
class ActionWiringRepositoryTest {

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
   * A nested JSON payload survives the round trip through a jsonb column. The payload goes in
   * non-canonical, with keys out of order and no spaces after the separators. The test reads it
   * back through {@link JdbcTemplate} to assert on the stored column value, not on what Hibernate
   * returns.
   *
   * <p>jsonb sorts object keys by length, then bytewise ({@code flag}, {@code scope}, {@code
   * appliesTo}), re-spaces the separators, and keeps array order. A {@code text} or {@code json}
   * column would return the input unchanged, so the reordering proves the column is jsonb.
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

  /** An Action that takes no parameters stores null params. */
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
   * Two Actions with the same {@code sort_order} on the same Transition would run in an undefined
   * order. The test saves through the repository to prove the constraint fires on a mapped save,
   * not only on raw SQL.
   *
   * <p>Both rows have the same {@code type_key} and differ only in {@code params}, which has no
   * constraint, so the refusal can only be about {@code sort_order}. One type key with two
   * configurations on one Transition is also the real case for post-visibility Actions.
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

  /** The unique index includes the Transition, so another Transition may reuse the order. */
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
   * {@code transition_id} is NOT NULL because Actions are always transition-scoped. The test inserts
   * through {@link JdbcTemplate} because the v1 seed is SQL that bypasses the mapping, and
   * Hibernate's {@code nullable = false} would hide whether the column itself has the constraint.
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
}
