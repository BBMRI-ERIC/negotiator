package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.OTHER_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.definitionIn;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.stateBuilder;
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
 * Persistence of the States of a Lifecycle Definition.
 *
 * <p>State names are unique within a definition because a stored State name is resolved through
 * the Definition Version Pin plus the name, and a duplicate would make that lookup ambiguous.
 */
@RepositoryTest
class StateRepositoryTest {

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
   * A Legacy State such as {@code APPROVED} or {@code RETURNED_FOR_RESUBMISSION} is the target and
   * source of no Transition. Neither the DDL nor the mapping requires a State to be reachable.
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
   * The two rows need different names, or the name constraint would refuse them first. The test
   * therefore names the index that must refuse the insert.
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
   * {@code label} is NOT NULL, so the Resource state-change notification never renders a null. The
   * SQL seed bypasses the mapping, so the test inserts through {@link JdbcTemplate} to check the
   * column itself.
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
   * The foreign key is {@code ON DELETE RESTRICT}, so deleting a definition that still has States
   * is refused instead of emptying its graph.
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

  /** The name is the natural key, so editing a State's label or terminal flag leaves it as is. */
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
}
