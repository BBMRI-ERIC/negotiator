package eu.bbmri_eric.negotiator.lifecycle.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Persistence behaviour of the Events of a Lifecycle Definition. As with States, name uniqueness
 * within a definition is a natural key the cutover depends on: it re-homes an Information
 * Requirement by matching a legacy {@code for_event} string against the Event of the same name.
 */
@RepositoryTest
class EventRepositoryTest {

  private static final String STANDARD_FAMILY = "standard-negotiation-flow";
  private static final String OTHER_FAMILY = "expedited-negotiation-flow";

  @Autowired EventRepository events;
  @Autowired LifecycleDefinitionRepository definitions;
  @Autowired EntityManager entityManager;

  private LifecycleDefinition definition;
  private LifecycleDefinition otherDefinition;

  @BeforeEach
  void setUp() {
    definition = definitions.saveAndFlush(definitionIn(STANDARD_FAMILY));
    otherDefinition = definitions.saveAndFlush(definitionIn(OTHER_FAMILY));
  }

  /**
   * An Event carrying no Transition is the Override Event's whole shape. The transition table does
   * not exist yet, so what this pins down today is that neither the DDL nor the mapping demands
   * one; slice 03 inherits the assertion.
   */
  @Test
  void save_withEveryColumnPopulatedAndNoTransition_roundTrips() {
    Event override =
        events.saveAndFlush(
            Event.builder().lifecycleDefinition(definition).name("OVERRIDE").build());
    entityManager.clear();

    Event found = events.findById(override.getId()).orElseThrow();
    assertNotNull(found.getId());
    assertEquals(definition.getId(), found.getLifecycleDefinition().getId());
    assertEquals("OVERRIDE", found.getName());
  }

  @Test
  void save_withANameAlreadyUsedInTheDefinition_isRefused() {
    events.saveAndFlush(eventIn(definition, "APPROVE"));

    Event duplicateName = eventIn(definition, "APPROVE");
    assertThrows(DataIntegrityViolationException.class, () -> events.saveAndFlush(duplicateName));
  }

  @Test
  void save_withTheSameNameInAnotherDefinition_isAccepted() {
    events.saveAndFlush(eventIn(definition, "APPROVE"));

    Event sameNameElsewhere = eventIn(otherDefinition, "APPROVE");
    assertNotNull(events.saveAndFlush(sameNameElsewhere).getId());
  }

  /** As for States: a referenced Definition Version is never discarded. */
  @Test
  void delete_aDefinitionThatHasAnEvent_isRefused() {
    events.saveAndFlush(eventIn(definition, "APPROVE"));
    entityManager.clear();

    LifecycleDefinition stored = definitions.findById(definition.getId()).orElseThrow();
    assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          definitions.delete(stored);
          definitions.flush();
        });
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
