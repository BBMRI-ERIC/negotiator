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

import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Persistence of the Transitions of a Lifecycle Definition.
 *
 * <p>The Transition Evaluator asks which Transition leaves State X for Event Y, so the answer must
 * be a single row. A Transition must also stay inside one Lifecycle Definition, or loading a graph
 * by its Definition Version id would return Transitions that point out of it.
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
                .requiredAuthority(Set.of(RequiredAuthority.IS_CREATOR))
                .build());
    entityManager.clear();

    Transition found = transitions.findById(transition.getId()).orElseThrow();
    assertNotNull(found.getId());
    assertEquals(definition.getId(), found.getLifecycleDefinition().getId());
    assertEquals(draft.getId(), found.getFromState().getId());
    assertEquals(submitted.getId(), found.getToState().getId());
    assertEquals(submit.getId(), found.getEvent().getId());
    assertEquals(Set.of(RequiredAuthority.IS_CREATOR), found.getRequiredAuthority());
  }

  /**
   * The column holds the enum's names, not its ordinals. The test reads it through {@link
   * JdbcTemplate} because an ordinal mapping would also round-trip through Hibernate, and the SQL
   * seed has to write the same spelling.
   */
  @Test
  void save_storesTheAuthorityAsItsName() {
    Transition transition = transitions.saveAndFlush(transitionOn(draft, submit, submitted));

    List<String> stored =
        jdbcTemplate.queryForList(
            "SELECT unnest(required_authority) FROM transition WHERE id = ?",
            String.class,
            transition.getId());
    assertEquals(List.of("IS_REPRESENTATIVE"), stored);
  }

  /** An empty set admits any person, and it comes back empty rather than null. */
  @Test
  void save_withAnEmptySet_roundTripsTheEmptySet() {
    Long id =
        transitions
            .saveAndFlush(
                Transition.builder()
                    .lifecycleDefinition(definition)
                    .fromState(draft)
                    .toState(submitted)
                    .event(submit)
                    .requiredAuthority(Set.of())
                    .build())
            .getId();
    entityManager.clear();

    assertEquals(Set.of(), transitions.findById(id).orElseThrow().getRequiredAuthority());
  }

  /** A set of more than one value, such as admin or creator, survives a round trip whole. */
  @Test
  void save_withASetOfTwoAuthorities_roundTripsTheSet() {
    Long id =
        transitions
            .saveAndFlush(
                Transition.builder()
                    .lifecycleDefinition(definition)
                    .fromState(draft)
                    .toState(submitted)
                    .event(submit)
                    .requiredAuthority(
                        Set.of(RequiredAuthority.IS_ADMIN, RequiredAuthority.IS_CREATOR))
                    .build())
            .getId();
    entityManager.clear();

    assertEquals(
        Set.of(RequiredAuthority.IS_ADMIN, RequiredAuthority.IS_CREATOR),
        transitions.findById(id).orElseThrow().getRequiredAuthority());
  }

  /** All four values of the enum are accepted, so the whitelist below is not narrower than it. */
  @Test
  void save_withEachOfTheFourAuthorities_isAccepted() {
    for (RequiredAuthority authority : RequiredAuthority.values()) {
      Event event = events.saveAndFlush(eventIn(definition, "EVENT_" + authority));
      Transition transition =
          transitions.saveAndFlush(
              Transition.builder()
                  .lifecycleDefinition(definition)
                  .fromState(draft)
                  .toState(submitted)
                  .event(event)
                  .requiredAuthority(Set.of(authority))
                  .build());
      assertNotNull(transition.getId());
    }
  }

  /**
   * The column refuses a fifth authority. The Java enum cannot represent one and the SQL seed
   * bypasses the mapping, so the refusal has to come from the column itself.
   */
  @Test
  void insert_withAnAuthorityOutsideTheFour_isRefusedByTheDatabase() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO transition (lifecycle_definition_id, from_state_id, to_state_id,"
                    + " event_id, required_authority) VALUES (?, ?, ?, ?, '{IS_MODERATOR}')",
                definition.getId(),
                draft.getId(),
                submitted.getId(),
                submit.getId()));
  }

  /**
   * {@code required_authority} is NOT NULL because an empty set already means any person may fire
   * the Event. The test inserts through {@link JdbcTemplate} so that Hibernate's {@code nullable =
   * false} cannot hide a missing column constraint.
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
   * The rest of the CHECK, each written as the SQL seed would write it. {@code NONE} is not a
   * value, {@code SYSTEM} only stands alone, and a NULL element or a nested array is not a set of
   * the four values.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {"{NONE}", "{SYSTEM,IS_ADMIN}", "{IS_ADMIN,NULL}", "{{IS_ADMIN},{IS_CREATOR}}"})
  void insert_withAnAuthoritySetTheCheckForbids_isRefusedByTheDatabase(String authorities) {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO transition (lifecycle_definition_id, from_state_id, to_state_id,"
                    + " event_id, required_authority) VALUES (?, ?, ?, ?, ?::VARCHAR[])",
                definition.getId(),
                draft.getId(),
                submitted.getId(),
                submit.getId(),
                authorities));
  }

  /** The control for the test above: the same insert, with a set the CHECK permits. */
  @ParameterizedTest
  @ValueSource(strings = {"{IS_ADMIN,IS_CREATOR,IS_REPRESENTATIVE}", "{SYSTEM}", "{}"})
  void insert_withASetTheCheckPermits_isAccepted(String authorities) {
    assertEquals(
        1,
        jdbcTemplate.update(
            "INSERT INTO transition (lifecycle_definition_id, from_state_id, to_state_id,"
                + " event_id, required_authority) VALUES (?, ?, ?, ?, ?::VARCHAR[])",
            definition.getId(),
            draft.getId(),
            submitted.getId(),
            submit.getId(),
            authorities));
  }

  /**
   * One {@code (source, event)} pair has at most one Transition, because the Transition Evaluator
   * cannot choose between two. The live graphs have no duplicate pairs.
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
            .requiredAuthority(Set.of(RequiredAuthority.IS_CREATOR))
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
            .requiredAuthority(Set.of(RequiredAuthority.IS_CREATOR))
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
            .requiredAuthority(Set.of(RequiredAuthority.IS_CREATOR))
            .build();
    assertThrows(DataIntegrityViolationException.class, () -> transitions.saveAndFlush(straddling));
  }

  /**
   * A Legacy State such as {@code APPROVED} or {@code RETURNED_FOR_RESUBMISSION} is the target of
   * no Transition and exists so that stored names and history still resolve. Nothing may require it
   * to be reachable.
   */
  @Test
  void save_aStateNoTransitionTargets_isAccepted() {
    transitions.saveAndFlush(transitionOn(draft, submit, submitted));

    State legacy = states.saveAndFlush(stateIn(definition, "APPROVED"));
    entityManager.clear();

    assertEquals("APPROVED", states.findById(legacy.getId()).orElseThrow().getName());
  }

  /**
   * The Override Event is only the name an admin's direct State change appears under in history,
   * so it has no Transition even when its definition has Transitions.
   */
  @Test
  void save_anEventNoTransitionReferences_isAccepted() {
    transitions.saveAndFlush(transitionOn(draft, submit, submitted));

    Event override = events.saveAndFlush(eventIn(definition, "OVERRIDE"));
    entityManager.clear();

    assertEquals("OVERRIDE", events.findById(override.getId()).orElseThrow().getName());
  }

  /** A State that a Transition uses cannot be deleted. */
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

  /** Editing who may fire a Transition leaves its source and target unchanged. */
  @Test
  void update_toTheRequiredAuthority_leavesTheReferencesUntouched() {
    Long id = transitions.saveAndFlush(transitionOn(draft, submit, submitted)).getId();
    entityManager.clear();

    Transition stored = transitions.findById(id).orElseThrow();
    stored.setRequiredAuthority(Set.of(RequiredAuthority.IS_ADMIN));
    transitions.saveAndFlush(stored);
    entityManager.clear();

    Transition reloaded = transitions.findById(id).orElseThrow();
    assertEquals(Set.of(RequiredAuthority.IS_ADMIN), reloaded.getRequiredAuthority());
    assertEquals(definition.getId(), reloaded.getLifecycleDefinition().getId());
    assertEquals(draft.getId(), reloaded.getFromState().getId());
    assertEquals(submitted.getId(), reloaded.getToState().getId());
    assertEquals(submit.getId(), reloaded.getEvent().getId());
  }

  /**
   * This test asserts on the schema because an index changes only how fast the lookup runs. An
   * {@code EXPLAIN} would not help, since the planner prefers a sequential scan on a table this
   * small.
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
        .requiredAuthority(Set.of(RequiredAuthority.IS_REPRESENTATIVE))
        .build();
  }
}
