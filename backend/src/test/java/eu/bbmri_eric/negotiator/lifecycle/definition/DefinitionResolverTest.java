package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.OTHER_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static org.junit.jupiter.api.Assertions.assertSame;
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

  @Mock private LifecycleDefinitionRepository definitions;

  @InjectMocks private DefinitionResolverImpl resolver;

  @Test
  void resolveForNegotiation_withOneActiveDefinition_returnsIt() {
    LifecycleDefinition sole =
        activeVersionIn(STANDARD_FAMILY, DefinitionScope.NEGOTIATION).build();
    when(definitions.findByScopeAndActiveTrue(DefinitionScope.NEGOTIATION))
        .thenReturn(List.of(sole));

    assertSame(sole, resolver.resolveForNegotiation());
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
  void resolveForResource_withAnActiveGlobalDefault_returnsIt() {
    LifecycleDefinition globalDefault =
        activeVersionIn(STANDARD_FAMILY, DefinitionScope.RESOURCE).globalDefault(true).build();
    when(definitions.findByScopeAndActiveTrueAndGlobalDefaultTrue(DefinitionScope.RESOURCE))
        .thenReturn(Optional.of(globalDefault));

    assertSame(globalDefault, resolver.resolveForResource());
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
