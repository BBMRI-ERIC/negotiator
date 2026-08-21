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
 * The decisions {@link DefinitionResolverImpl} makes once the rows are in front of it: which of the
 * two questions it asks the table, and what it does when the answer is not exactly one Lifecycle
 * Definition. The repository is mocked, so nothing here depends on a database.
 *
 * <p>Which rows each of those two queries actually selects is a different question and is proven
 * against a real PostgreSQL in {@link LifecycleDefinitionRepositoryTest} — a mock would happily
 * return an inactive row from a finder whose name promises active ones.
 *
 * <p>The two refusal tests are not edge cases. The schema holds no definitions at all until the v1
 * seed lands, so failing to resolve is the <em>only</em> thing this resolver can currently do, and
 * how it fails is the whole of its observable behaviour today.
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
   * Nothing in the schema keeps a second Negotiation-scope family from having an active version —
   * the partial unique index is per family — so the resolver is the only place the "sole" in "the
   * sole active Negotiation definition" is enforced, and the refusal has to say which families are
   * in the way for an admin to be able to act on it.
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
   * Version 1 of {@code familyKey}, active, and not the global default. Inline because {@link
   * DefinitionFixtures} holds only what more than one test class needs, and because folding this
   * into {@link LifecycleDefinitionRepositoryTest}'s own {@code versionBuilder} would change that
   * class's fixture under it: that one fixes the name and parameterizes the version, where this one
   * does the opposite.
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
