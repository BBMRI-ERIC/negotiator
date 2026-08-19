package eu.bbmri_eric.negotiator.lifecycle.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@RepositoryTest
class LifecycleDefinitionRepositoryTest {

  private static final String STANDARD_FAMILY = "standard-negotiation-flow";
  private static final String OTHER_FAMILY = "expedited-negotiation-flow";

  @Autowired LifecycleDefinitionRepository repository;
  @Autowired EntityManager entityManager;

  @Test
  void save_withEveryColumnPopulated_roundTrips() {
    LifecycleDefinition definition =
        repository.saveAndFlush(
            LifecycleDefinition.builder()
                .scope(DefinitionScope.NEGOTIATION)
                .familyKey(STANDARD_FAMILY)
                .name("Standard flow")
                .version(3)
                .active(true)
                .globalDefault(true)
                .build());
    entityManager.clear();

    LifecycleDefinition found = repository.findById(definition.getId()).orElseThrow();
    assertNotNull(found.getId());
    assertEquals(DefinitionScope.NEGOTIATION, found.getScope());
    assertEquals(STANDARD_FAMILY, found.getFamilyKey());
    assertEquals("Standard flow", found.getName());
    assertEquals(3, found.getVersion());
    assertTrue(found.isActive());
    assertTrue(found.isGlobalDefault());
  }

  @Test
  void save_withScope_persistsTheEnumNameAndNotItsOrdinal() {
    LifecycleDefinition definition =
        repository.saveAndFlush(
            versionBuilder(OTHER_FAMILY, 1).scope(DefinitionScope.RESOURCE).build());

    Object storedScope =
        entityManager
            .createNativeQuery("SELECT scope FROM lifecycle_definition WHERE id = :id")
            .setParameter("id", definition.getId())
            .getSingleResult();
    assertEquals("RESOURCE", storedScope);
  }

  @Test
  void save_withoutFlags_defaultsToInactiveAndNotGlobalDefault() {
    LifecycleDefinition definition =
        repository.saveAndFlush(versionBuilder(STANDARD_FAMILY, 1).build());
    entityManager.clear();

    LifecycleDefinition found = repository.findById(definition.getId()).orElseThrow();
    assertFalse(found.isActive());
    assertFalse(found.isGlobalDefault());
  }

  @Test
  void save_withAVersionNumberAlreadyUsedInTheFamily_isRefused() {
    repository.saveAndFlush(versionBuilder(STANDARD_FAMILY, 1).build());

    LifecycleDefinition reusedVersion =
        versionBuilder(STANDARD_FAMILY, 1).name("Rewritten").build();
    assertThrows(
        DataIntegrityViolationException.class, () -> repository.saveAndFlush(reusedVersion));
  }

  @Test
  void save_withTheSameVersionNumberInAnotherFamily_isAccepted() {
    repository.saveAndFlush(versionBuilder(STANDARD_FAMILY, 1).build());

    LifecycleDefinition otherFamily = versionBuilder(OTHER_FAMILY, 1).build();
    assertNotNull(repository.saveAndFlush(otherFamily).getId());
  }

  @Test
  void save_withASecondActiveVersionInTheSameFamily_isRefused() {
    repository.saveAndFlush(versionBuilder(STANDARD_FAMILY, 1).active(true).build());

    LifecycleDefinition secondActive = versionBuilder(STANDARD_FAMILY, 2).active(true).build();
    assertThrows(
        DataIntegrityViolationException.class, () -> repository.saveAndFlush(secondActive));
  }

  @Test
  void save_withAnActiveVersionInAnotherFamily_isAccepted() {
    repository.saveAndFlush(versionBuilder(STANDARD_FAMILY, 1).active(true).build());

    LifecycleDefinition otherFamilyActive = versionBuilder(OTHER_FAMILY, 1).active(true).build();
    assertNotNull(repository.saveAndFlush(otherFamilyActive).getId());
  }

  @Test
  void save_withTwoInactiveVersionsInTheSameFamily_isAccepted() {
    repository.saveAndFlush(versionBuilder(STANDARD_FAMILY, 1).build());

    LifecycleDefinition secondInactive = versionBuilder(STANDARD_FAMILY, 2).build();
    assertNotNull(repository.saveAndFlush(secondInactive).getId());
  }

  @Test
  void save_withASecondActiveGlobalDefault_isRefused() {
    repository.saveAndFlush(
        versionBuilder(STANDARD_FAMILY, 1).active(true).globalDefault(true).build());

    LifecycleDefinition secondGlobalDefault =
        versionBuilder(OTHER_FAMILY, 1).active(true).globalDefault(true).build();
    assertThrows(
        DataIntegrityViolationException.class, () -> repository.saveAndFlush(secondGlobalDefault));
  }

  /**
   * The flag travels with the family across its versions, so a superseded version keeping it must
   * not collide with the active one.
   */
  @Test
  void save_withAnInactiveVersionCarryingTheGlobalDefault_isAccepted() {
    repository.saveAndFlush(
        versionBuilder(STANDARD_FAMILY, 1).active(true).globalDefault(true).build());

    LifecycleDefinition supersededDefault =
        versionBuilder(STANDARD_FAMILY, 2).globalDefault(true).build();
    assertNotNull(repository.saveAndFlush(supersededDefault).getId());
  }

  @Test
  void update_toTheEditableFields_leavesTheIdentityColumnsUntouched() {
    Long id =
        repository
            .saveAndFlush(versionBuilder(STANDARD_FAMILY, 2).name("Standard flow").build())
            .getId();
    entityManager.clear();

    LifecycleDefinition stored = repository.findById(id).orElseThrow();
    stored.setName("Standard flow (renamed)");
    stored.setActive(true);
    stored.setGlobalDefault(true);
    repository.saveAndFlush(stored);
    entityManager.clear();

    LifecycleDefinition reloaded = repository.findById(id).orElseThrow();
    assertEquals(id, reloaded.getId());
    assertEquals(STANDARD_FAMILY, reloaded.getFamilyKey());
    assertEquals(2, reloaded.getVersion());
    assertEquals(DefinitionScope.NEGOTIATION, reloaded.getScope());
    assertEquals("Standard flow (renamed)", reloaded.getName());
    assertTrue(reloaded.isActive());
    assertTrue(reloaded.isGlobalDefault());
  }

  private static LifecycleDefinition.LifecycleDefinitionBuilder versionBuilder(
      String familyKey, int version) {
    return LifecycleDefinition.builder()
        .scope(DefinitionScope.NEGOTIATION)
        .familyKey(familyKey)
        .name("Standard flow")
        .version(version);
  }
}
