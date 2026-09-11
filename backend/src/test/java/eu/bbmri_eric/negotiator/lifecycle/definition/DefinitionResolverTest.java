package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.OTHER_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The decisions {@link DefinitionResolverImpl} makes once it has the rows: which of the two queries
 * it runs, and what it does when the answer is not exactly one Definition Version. The repository
 * is mocked, so nothing here needs a database.
 *
 * <p>{@link LifecycleDefinitionRepositoryTest} checks against a real PostgreSQL which rows each
 * query selects, because a mock would return an inactive row from a finder that promises active
 * ones.
 */
@ExtendWith(MockitoExtension.class)
class DefinitionResolverTest {

  /** The row id both queries should answer with, set explicitly instead of left to a default. */
  private static final long SOLE_VERSION_ID = 17L;

  @Mock private LifecycleDefinitionRepository definitions;

  @InjectMocks private DefinitionResolverImpl resolver;

  /**
   * The answer is the row id, not the row, because the caller writes that id to a Definition
   * Version Pin. The test asserts against an id it set, so a resolver answering any version would
   * fail.
   */
  @Test
  void resolveForNegotiation_withOneActiveDefinition_answersItsRowId() {
    LifecycleDefinition sole =
        activeVersionIn(STANDARD_FAMILY, DefinitionScope.NEGOTIATION).id(SOLE_VERSION_ID).build();
    when(definitions.findByScopeAndActiveTrue(DefinitionScope.NEGOTIATION))
        .thenReturn(List.of(sole));

    assertEquals(SOLE_VERSION_ID, resolver.resolveForNegotiation());
  }

  @Test
  void resolveForNegotiation_withNoActiveDefinition_isRefused() {
    when(definitions.findByScopeAndActiveTrue(DefinitionScope.NEGOTIATION)).thenReturn(List.of());

    assertThrows(DefinitionResolutionException.class, () -> resolver.resolveForNegotiation());
  }

  /**
   * The partial unique index is per family, so the schema allows a second Negotiation-scope family
   * with an active version. Only the resolver enforces a single active Negotiation definition, and
   * the refusal names the families so an admin can act on it.
   */
  @Test
  void resolveForNegotiation_withMoreThanOneActiveDefinition_isRefusedAndNamesTheFamilies() {
    when(definitions.findByScopeAndActiveTrue(DefinitionScope.NEGOTIATION))
        .thenReturn(
            List.of(
                activeVersionIn(STANDARD_FAMILY, DefinitionScope.NEGOTIATION).build(),
                activeVersionIn(OTHER_FAMILY, DefinitionScope.NEGOTIATION).build()));

    DefinitionResolutionException refused =
        assertThrows(DefinitionResolutionException.class, () -> resolver.resolveForNegotiation());
    assertTrue(refused.getMessage().contains(STANDARD_FAMILY));
    assertTrue(refused.getMessage().contains(OTHER_FAMILY));
  }

  @Test
  void resolveForResource_withAnActiveGlobalDefault_answersItsRowId() {
    LifecycleDefinition globalDefault =
        activeVersionIn(STANDARD_FAMILY, DefinitionScope.RESOURCE)
            .id(SOLE_VERSION_ID)
            .globalDefault(true)
            .build();
    when(definitions.findByScopeAndActiveTrueAndGlobalDefaultTrue(DefinitionScope.RESOURCE))
        .thenReturn(Optional.of(globalDefault));

    assertEquals(SOLE_VERSION_ID, resolver.resolveForResource());
  }

  @Test
  void resolveForResource_withoutAnActiveGlobalDefault_isRefused() {
    when(definitions.findByScopeAndActiveTrueAndGlobalDefaultTrue(DefinitionScope.RESOURCE))
        .thenReturn(Optional.empty());

    assertThrows(DefinitionResolutionException.class, () -> resolver.resolveForResource());
  }

  /**
   * Version 1 of {@code familyKey}, active, and not the global default. It stays here because
   * {@link LifecycleDefinitionRepositoryTest}'s {@code versionBuilder} fixes the name and varies
   * the version, the opposite of this one.
   */
  private static LifecycleDefinition.LifecycleDefinitionBuilder activeVersionIn(
      String familyKey, DefinitionScope scope) {
    return LifecycleDefinition.builder()
        .scope(scope)
        .familyKey(familyKey)
        .name(familyKey)
        .version(1)
        .active(true);
  }
}
