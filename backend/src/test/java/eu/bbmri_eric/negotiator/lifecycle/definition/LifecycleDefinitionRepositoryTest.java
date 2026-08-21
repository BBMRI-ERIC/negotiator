package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.OTHER_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Persistence behaviour of the root table of the Lifecycle Definition schema. Every test here
 * asserts something a caller can observe from outside — a row survives a round trip, or a write is
 * refused — never a mapping annotation.
 *
 * <p>The refusal tests exist because publishing a malformed definition has to be impossible rather
 * than merely discouraged, and because these are the first unique and first partial indexes in this
 * codebase: there is no prior art whose syntax can be trusted to read correctly.
 *
 * <p>The finder tests at the end are the other half of {@link DefinitionResolverTest}: they are
 * about which rows the two queries Definition Resolution runs actually select, which a mocked
 * repository cannot show.
 */
@RepositoryTest
class LifecycleDefinitionRepositoryTest {

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
   * The flag belongs to the family and travels across its versions, so a superseded version keeping
   * it must not collide with the active one that now carries it.
   */
  @Test
  void save_withAnInactiveVersionCarryingTheGlobalDefault_isAccepted() {
    repository.saveAndFlush(
        versionBuilder(STANDARD_FAMILY, 1).active(true).globalDefault(true).build());

    LifecycleDefinition supersededDefault =
        versionBuilder(STANDARD_FAMILY, 2).globalDefault(true).build();
    assertNotNull(repository.saveAndFlush(supersededDefault).getId());
  }

  /**
   * The row id is the sole machine identity: editing a family's display label or flipping which
   * version is active must leave the family_key and the version integer exactly where they were, so
   * that nothing already pointing at the row is moved by an edit.
   */
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

  @Test
  void findByScopeAndActiveTrue_returnsTheActiveVersionAndNotTheSupersededOne() {
    Long active =
        repository.saveAndFlush(versionBuilder(STANDARD_FAMILY, 2).active(true).build()).getId();
    repository.saveAndFlush(versionBuilder(STANDARD_FAMILY, 1).build());
    entityManager.clear();

    List<LifecycleDefinition> found =
        repository.findByScopeAndActiveTrue(DefinitionScope.NEGOTIATION);
    assertEquals(1, found.size());
    assertEquals(active, found.get(0).getId());
  }

  /**
   * The database enforces one active version per <em>family</em>, so "the sole active Negotiation
   * definition" is not a guarantee it makes. This is the input Definition Resolution has to refuse
   * rather than pick from, and it is reachable.
   */
  @Test
  void findByScopeAndActiveTrue_returnsAnActiveVersionOfEveryFamilyOfTheScope() {
    repository.saveAndFlush(versionBuilder(STANDARD_FAMILY, 1).active(true).build());
    repository.saveAndFlush(versionBuilder(OTHER_FAMILY, 1).active(true).build());
    entityManager.clear();

    assertEquals(2, repository.findByScopeAndActiveTrue(DefinitionScope.NEGOTIATION).size());
  }

  @Test
  void findByScopeAndActiveTrue_ignoresAnActiveVersionOfTheOtherScope() {
    repository.saveAndFlush(
        versionBuilder(OTHER_FAMILY, 1).scope(DefinitionScope.RESOURCE).active(true).build());
    entityManager.clear();

    assertTrue(repository.findByScopeAndActiveTrue(DefinitionScope.NEGOTIATION).isEmpty());
  }

  /**
   * The second family here is active too, and is not the global default: being the family the flag
   * is on is what makes a definition the one a Resource with no closer association resolves to.
   */
  @Test
  void findByScopeAndActiveTrueAndGlobalDefaultTrue_returnsTheFlaggedFamilysActiveVersion() {
    Long globalDefault =
        repository
            .saveAndFlush(
                versionBuilder(STANDARD_FAMILY, 1)
                    .scope(DefinitionScope.RESOURCE)
                    .active(true)
                    .globalDefault(true)
                    .build())
            .getId();
    repository.saveAndFlush(
        versionBuilder(OTHER_FAMILY, 1).scope(DefinitionScope.RESOURCE).active(true).build());
    entityManager.clear();

    LifecycleDefinition found =
        repository
            .findByScopeAndActiveTrueAndGlobalDefaultTrue(DefinitionScope.RESOURCE)
            .orElseThrow();
    assertEquals(globalDefault, found.getId());
  }

  /**
   * A superseded version keeps the flag rather than having it moved off it, so the query has to
   * filter on activeness as well and not just on the flag.
   */
  @Test
  void findByScopeAndActiveTrueAndGlobalDefaultTrue_ignoresASupersededDefault() {
    repository.saveAndFlush(
        versionBuilder(STANDARD_FAMILY, 1)
            .scope(DefinitionScope.RESOURCE)
            .globalDefault(true)
            .build());
    entityManager.clear();

    assertTrue(
        repository
            .findByScopeAndActiveTrueAndGlobalDefaultTrue(DefinitionScope.RESOURCE)
            .isEmpty());
  }

  /**
   * The partial unique index allows one active global default in the whole table rather than one
   * per scope, so without the scope in the query a Negotiation-scope family carrying the flag would
   * be handed back as the Resource default.
   */
  @Test
  void findByScopeAndActiveTrueAndGlobalDefaultTrue_ignoresOneOfTheOtherScope() {
    repository.saveAndFlush(
        versionBuilder(STANDARD_FAMILY, 1).active(true).globalDefault(true).build());
    entityManager.clear();

    assertTrue(
        repository
            .findByScopeAndActiveTrueAndGlobalDefaultTrue(DefinitionScope.RESOURCE)
            .isEmpty());
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
