package eu.bbmri_eric.negotiator.lifecycle.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Persistence behaviour of the States of a Lifecycle Definition. Every test asserts something a
 * caller can observe from outside — a row survives a round trip, or a write is refused.
 *
 * <p>Name uniqueness within a definition is load-bearing rather than hygiene: the cutover resolves
 * a live state string through the Definition Version Pin plus the state name, so a duplicate would
 * make that lookup ambiguous. The refusal tests exist because these are among the first unique and
 * partial indexes in this codebase, so no syntax here can be trusted to read correctly.
 */
@RepositoryTest
class StateRepositoryTest {

  private static final String STANDARD_FAMILY = "standard-negotiation-flow";
  private static final String OTHER_FAMILY = "expedited-negotiation-flow";

  @Autowired StateRepository states;
  @Autowired LifecycleDefinitionRepository definitions;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbcTemplate;

  private LifecycleDefinition definition;
  private LifecycleDefinition otherDefinition;

  @BeforeEach
  void setUp() {
    definition = definitions.saveAndFlush(definitionIn(STANDARD_FAMILY));
    otherDefinition = definitions.saveAndFlush(definitionIn(OTHER_FAMILY));
  }

  @Test
  void save_withEveryColumnPopulated_roundTrips() {
    State state =
        states.saveAndFlush(
            State.builder()
                .lifecycleDefinition(definition)
                .name("SUBMITTED")
                .label("Submitted")
                .initial(true)
                .terminal(false)
                .build());
    entityManager.clear();

    State found = states.findById(state.getId()).orElseThrow();
    assertNotNull(found.getId());
    assertEquals(definition.getId(), found.getLifecycleDefinition().getId());
    assertEquals("SUBMITTED", found.getName());
    assertEquals("Submitted", found.getLabel());
    assertTrue(found.isInitial());
    assertFalse(found.isTerminal());
  }

  @Test
  void save_withoutFlags_defaultsToNeitherInitialNorTerminal() {
    State state = states.saveAndFlush(stateBuilder(definition, "IN_PROGRESS").build());
    entityManager.clear();

    State found = states.findById(state.getId()).orElseThrow();
    assertFalse(found.isInitial());
    assertFalse(found.isTerminal());
  }

  /**
   * A Legacy State — {@code APPROVED} and {@code RETURNED_FOR_RESUBMISSION} are the target of no
   * Transition and the source of none. The transition table does not exist yet, so what this pins
   * down today is that neither the DDL nor the mapping demands a State be reachable; slice 03
   * inherits the assertion.
   */
  @Test
  void save_aStateNoTransitionTargets_roundTrips() {
    State legacy =
        states.saveAndFlush(
            stateBuilder(definition, "APPROVED").label("Approved").terminal(true).build());
    entityManager.clear();

    State found = states.findById(legacy.getId()).orElseThrow();
    assertEquals("APPROVED", found.getName());
    assertEquals("Approved", found.getLabel());
    assertTrue(found.isTerminal());
  }

  /**
   * The two rows must differ in their names — a second row named {@code SUBMITTED} would trip the
   * name constraint instead — so this is the one refusal here whose rows cannot differ only in the
   * constrained dimension. It therefore names the index that refused it rather than leaving
   * attribution to a reading of the DDL.
   */
  @Test
  void save_withASecondInitialStateInTheSameDefinition_isRefused() {
    states.saveAndFlush(stateBuilder(definition, "SUBMITTED").initial(true).build());

    State secondInitial = stateBuilder(definition, "DRAFT").initial(true).build();
    DataIntegrityViolationException refused =
        assertThrows(
            DataIntegrityViolationException.class, () -> states.saveAndFlush(secondInitial));
    assertTrue(refused.getMessage().contains("uq_state_initial_per_definition"));
  }

  @Test
  void save_withAnInitialStateInAnotherDefinition_isAccepted() {
    states.saveAndFlush(stateBuilder(definition, "SUBMITTED").initial(true).build());

    State otherInitial = stateBuilder(otherDefinition, "SUBMITTED").initial(true).build();
    assertNotNull(states.saveAndFlush(otherInitial).getId());
  }

  /** The index is partial, so it must not constrain the States that are not initial. */
  @Test
  void save_withTwoNonInitialStatesInTheSameDefinition_isAccepted() {
    states.saveAndFlush(stateBuilder(definition, "ABANDONED").build());

    State secondNonInitial = stateBuilder(definition, "DECLINED").build();
    assertNotNull(states.saveAndFlush(secondNonInitial).getId());
  }

  @Test
  void save_withANameAlreadyUsedInTheDefinition_isRefused() {
    states.saveAndFlush(stateBuilder(definition, "SUBMITTED").build());

    State duplicateName = stateBuilder(definition, "SUBMITTED").build();
    assertThrows(DataIntegrityViolationException.class, () -> states.saveAndFlush(duplicateName));
  }

  @Test
  void save_withTheSameNameInAnotherDefinition_isAccepted() {
    states.saveAndFlush(stateBuilder(definition, "SUBMITTED").build());

    State sameNameElsewhere = stateBuilder(otherDefinition, "SUBMITTED").build();
    assertNotNull(states.saveAndFlush(sameNameElsewhere).getId());
  }

  /**
   * {@code label} is NOT NULL. The v1 seed is SQL and bypasses the mapping entirely, so the refusal
   * that protects the Resource state-change notification body from rendering nulls in a sentence
   * has to come from the column rather than from Hibernate. Inserted through {@link JdbcTemplate}
   * for that reason.
   */
  @Test
  void insert_withoutALabel_isRefusedByTheDatabase() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO state (lifecycle_definition_id, name, label) VALUES (?, ?, NULL)",
                definition.getId(),
                "SUBMITTED"));
  }

  /**
   * A Definition Version that is referenced is never discarded, so the foreign key is {@code ON
   * DELETE RESTRICT} and deleting the definition out from under its States is refused rather than
   * cascading into a silently emptied graph.
   */
  @Test
  void delete_aDefinitionThatHasAState_isRefused() {
    states.saveAndFlush(stateBuilder(definition, "SUBMITTED").build());
    entityManager.clear();

    LifecycleDefinition stored = definitions.findById(definition.getId()).orElseThrow();
    assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          definitions.delete(stored);
          definitions.flush();
        });
  }

  /**
   * The name is the natural key, so editing what a State displays or where a Lifecycle may finish
   * must leave it — and the definition the State belongs to — exactly where they were.
   */
  @Test
  void update_toTheEditableFields_leavesTheNameAndDefinitionUntouched() {
    Long id = states.saveAndFlush(stateBuilder(definition, "SUBMITTED").build()).getId();
    entityManager.clear();

    State stored = states.findById(id).orElseThrow();
    stored.setLabel("Submitted for review");
    stored.setTerminal(true);
    states.saveAndFlush(stored);
    entityManager.clear();

    State reloaded = states.findById(id).orElseThrow();
    assertEquals("SUBMITTED", reloaded.getName());
    assertEquals(definition.getId(), reloaded.getLifecycleDefinition().getId());
    assertEquals("Submitted for review", reloaded.getLabel());
    assertTrue(reloaded.isTerminal());
  }

  private static State.StateBuilder stateBuilder(LifecycleDefinition definition, String name) {
    return State.builder().lifecycleDefinition(definition).name(name).label(name);
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
