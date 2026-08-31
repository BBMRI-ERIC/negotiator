package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.OTHER_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.definitionIn;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.eventIn;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.stateIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.lifecycle.RequiredAuthority;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Persistence behaviour of the Transitions of a Lifecycle Definition — the edges of the graph.
 * Every test asserts something a caller can observe from outside: a row survives a round trip, or a
 * write is refused.
 *
 * <p>Two properties here are load-bearing for the Transition Evaluator rather than hygiene. Its
 * whole question is "which Transition leaves State X for Event Y", so that answer must be a single
 * row — a duplicate would be an ambiguity it has no way to resolve. And a Transition must not be
 * able to straddle two Lifecycle Definitions, or loading a graph by its Definition Version id would
 * return edges pointing out of it.
 */
@RepositoryTest
class TransitionRepositoryTest {

  @Autowired TransitionRepository transitions;
  @Autowired StateRepository states;
  @Autowired EventRepository events;
  @Autowired LifecycleDefinitionRepository definitions;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbcTemplate;

  private LifecycleDefinition definition;
  private State draft;
  private State submitted;
  private Event submit;

  private LifecycleDefinition otherDefinition;
  private State otherDraft;
  private Event otherSubmit;

  @BeforeEach
  void setUp() {
    definition = definitions.saveAndFlush(definitionIn(STANDARD_FAMILY));
    draft = states.saveAndFlush(stateIn(definition, "DRAFT"));
    submitted = states.saveAndFlush(stateIn(definition, "SUBMITTED"));
    submit = events.saveAndFlush(eventIn(definition, "SUBMIT"));

    otherDefinition = definitions.saveAndFlush(definitionIn(OTHER_FAMILY));
    otherDraft = states.saveAndFlush(stateIn(otherDefinition, "DRAFT"));
    otherSubmit = events.saveAndFlush(eventIn(otherDefinition, "SUBMIT"));
  }

  @Test
  void save_withEveryReferencePopulated_roundTripsAllFive() {
    Transition transition =
        transitions.saveAndFlush(
            Transition.builder()
                .lifecycleDefinition(definition)
                .fromState(draft)
                .toState(submitted)
                .event(submit)
                .requiredAuthority(RequiredAuthority.IS_CREATOR)
                .build());
    entityManager.clear();

    Transition found = transitions.findById(transition.getId()).orElseThrow();
    assertNotNull(found.getId());
    assertEquals(definition.getId(), found.getLifecycleDefinition().getId());
    assertEquals(draft.getId(), found.getFromState().getId());
    assertEquals(submitted.getId(), found.getToState().getId());
    assertEquals(submit.getId(), found.getEvent().getId());
    assertEquals(RequiredAuthority.IS_CREATOR, found.getRequiredAuthority());
  }

  /**
   * The column holds the enum's name, not its ordinal. Read back through {@link JdbcTemplate}
   * rather than through the mapping, because {@code @Enumerated(ORDINAL)} would round-trip through
   * Hibernate just as happily and the seed is SQL that has to write the same spelling.
   */
  @Test
  void save_storesTheAuthorityAsItsName() {
    Transition transition = transitions.saveAndFlush(transitionOn(draft, submit, submitted));

    String stored =
        jdbcTemplate.queryForObject(
            "SELECT required_authority FROM transition WHERE id = ?",
            String.class,
            transition.getId());
    assertEquals("IS_REPRESENTATIVE", stored);
  }

  /** All five values of the enum are accepted, so the whitelist below is not narrower than it. */
  @Test
  void save_withEachOfTheFiveAuthorities_isAccepted() {
    for (RequiredAuthority authority : RequiredAuthority.values()) {
      Event event = events.saveAndFlush(eventIn(definition, "EVENT_" + authority));
      Transition transition =
          transitions.saveAndFlush(
              Transition.builder()
                  .lifecycleDefinition(definition)
                  .fromState(draft)
                  .toState(submitted)
                  .event(event)
                  .requiredAuthority(authority)
                  .build());
      assertNotNull(transition.getId());
    }
  }

  /**
   * A sixth authority is refused. Inserted through {@link JdbcTemplate} because the Java enum makes
   * the value unrepresentable through the mapping, and the v1 seed is SQL that bypasses the mapping
   * entirely — so the refusal has to come from the column itself.
   */
  @Test
  void insert_withAnAuthorityOutsideTheFive_isRefusedByTheDatabase() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO transition (lifecycle_definition_id, from_state_id, to_state_id,"
                    + " event_id, required_authority) VALUES (?, ?, ?, ?, 'IS_MODERATOR')",
                definition.getId(),
                draft.getId(),
                submitted.getId(),
                submit.getId()));
  }

  /**
   * {@code required_authority} is NOT NULL: {@link RequiredAuthority#NONE} is the spelling for
   * "anyone may fire this", so a null would be a second, silent way to say the same thing. Inserted
   * through {@link JdbcTemplate} for the same reason as the test above — the v1 seed is SQL and
   * bypasses the mapping, so Hibernate's {@code nullable = false} would otherwise mask whether the
   * column itself carries the constraint.
   */
  @Test
  void insert_withoutAnAuthority_isRefusedByTheDatabase() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO transition (lifecycle_definition_id, from_state_id, to_state_id,"
                    + " event_id, required_authority) VALUES (?, ?, ?, ?, NULL)",
                definition.getId(),
                draft.getId(),
                submitted.getId(),
                submit.getId()));
  }

  /**
   * The evaluator's answer to "which Transition leaves State X for Event Y" must be a single row.
   * The frozen graph dump has 0 duplicate {@code (source, event)} pairs across both live graphs, so
   * this constrains nothing real and forecloses an ambiguity the evaluator could not resolve.
   */
  @Test
  void save_withASourceAndEventAlreadyUsed_isRefused() {
    transitions.saveAndFlush(transitionOn(draft, submit, submitted));

    Transition duplicate = transitionOn(draft, submit, draft);
    assertThrows(DataIntegrityViolationException.class, () -> transitions.saveAndFlush(duplicate));
  }

  @Test
  void save_withTheSameEventFromAnotherState_isAccepted() {
    transitions.saveAndFlush(transitionOn(draft, submit, submitted));

    Transition fromElsewhere = transitionOn(submitted, submit, draft);
    assertNotNull(transitions.saveAndFlush(fromElsewhere).getId());
  }

  @Test
  void save_withAnotherEventFromTheSameState_isAccepted() {
    transitions.saveAndFlush(transitionOn(draft, submit, submitted));

    Event abandon = events.saveAndFlush(eventIn(definition, "ABANDON"));
    Transition onAnotherEvent = transitionOn(draft, abandon, submitted);
    assertNotNull(transitions.saveAndFlush(onAnotherEvent).getId());
  }

  @Test
  void save_withAFromStateFromAnotherDefinition_isRefused() {
    Transition straddling =
        Transition.builder()
            .lifecycleDefinition(definition)
            .fromState(otherDraft)
            .toState(submitted)
            .event(submit)
            .requiredAuthority(RequiredAuthority.NONE)
            .build();
    assertThrows(DataIntegrityViolationException.class, () -> transitions.saveAndFlush(straddling));
  }

  @Test
  void save_withAToStateFromAnotherDefinition_isRefused() {
    Transition straddling =
        Transition.builder()
            .lifecycleDefinition(definition)
            .fromState(draft)
            .toState(otherDraft)
            .event(submit)
            .requiredAuthority(RequiredAuthority.NONE)
            .build();
    assertThrows(DataIntegrityViolationException.class, () -> transitions.saveAndFlush(straddling));
  }

  @Test
  void save_withAnEventFromAnotherDefinition_isRefused() {
    Transition straddling =
        Transition.builder()
            .lifecycleDefinition(definition)
            .fromState(draft)
            .toState(submitted)
            .event(otherSubmit)
            .requiredAuthority(RequiredAuthority.NONE)
            .build();
    assertThrows(DataIntegrityViolationException.class, () -> transitions.saveAndFlush(straddling));
  }

  /**
   * A Legacy State — {@code APPROVED} and {@code RETURNED_FOR_RESUBMISSION} are the target of no
   * Transition, and exist so that live strings and audit history still resolve after the cutover.
   * Slice 02 could only pin this down with no transition table in existence; now that there is one,
   * and a Transition in the same definition, nothing may demand the State be reachable.
   */
  @Test
  void save_aStateNoTransitionTargets_isAccepted() {
    transitions.saveAndFlush(transitionOn(draft, submit, submitted));

    State legacy = states.saveAndFlush(stateIn(definition, "APPROVED"));
    entityManager.clear();

    assertEquals("APPROVED", states.findById(legacy.getId()).orElseThrow().getName());
  }

  /**
   * The Override Event's whole shape: a name under which an admin's direct state change appears in
   * history, carrying no Transition even though its definition has Transitions.
   */
  @Test
  void save_anEventNoTransitionReferences_isAccepted() {
    transitions.saveAndFlush(transitionOn(draft, submit, submitted));

    Event override = events.saveAndFlush(eventIn(definition, "OVERRIDE"));
    entityManager.clear();

    assertEquals("OVERRIDE", events.findById(override.getId()).orElseThrow().getName());
  }

  /** A State an edge depends on is never discarded out from under it. */
  @Test
  void delete_aStateATransitionReferences_isRefused() {
    transitions.saveAndFlush(transitionOn(draft, submit, submitted));
    entityManager.clear();

    State stored = states.findById(draft.getId()).orElseThrow();
    assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          states.delete(stored);
          states.flush();
        });
  }

  /** Editing who may fire an edge must leave the edge itself exactly where it was. */
  @Test
  void update_toTheRequiredAuthority_leavesTheReferencesUntouched() {
    Long id = transitions.saveAndFlush(transitionOn(draft, submit, submitted)).getId();
    entityManager.clear();

    Transition stored = transitions.findById(id).orElseThrow();
    stored.setRequiredAuthority(RequiredAuthority.IS_ADMIN);
    transitions.saveAndFlush(stored);
    entityManager.clear();

    Transition reloaded = transitions.findById(id).orElseThrow();
    assertEquals(RequiredAuthority.IS_ADMIN, reloaded.getRequiredAuthority());
    assertEquals(definition.getId(), reloaded.getLifecycleDefinition().getId());
    assertEquals(draft.getId(), reloaded.getFromState().getId());
    assertEquals(submitted.getId(), reloaded.getToState().getId());
    assertEquals(submit.getId(), reloaded.getEvent().getId());
  }

  /**
   * The one assertion here about the schema rather than about behaviour, because an index has no
   * behavioural signature to assert on: it changes how fast the evaluator's lookup runs, not what
   * it returns. The alternative — an {@code EXPLAIN} asserting an index scan — is worse, since the
   * planner rightly prefers a sequential scan on a table this small.
   */
  @Test
  void theLookupTheEvaluatorLivesOn_isIndexed() {
    List<String> definitions =
        jdbcTemplate.queryForList(
            "SELECT indexdef FROM pg_indexes WHERE tablename = 'transition'", String.class);

    assertTrue(
        definitions.stream().anyMatch(it -> it.contains("(from_state_id, event_id)")),
        "no index leads with (from_state_id, event_id); found " + definitions);
  }

  private Transition transitionOn(State fromState, Event event, State toState) {
    return Transition.builder()
        .lifecycleDefinition(definition)
        .fromState(fromState)
        .toState(toState)
        .event(event)
        .requiredAuthority(RequiredAuthority.IS_REPRESENTATIVE)
        .build();
  }
}
