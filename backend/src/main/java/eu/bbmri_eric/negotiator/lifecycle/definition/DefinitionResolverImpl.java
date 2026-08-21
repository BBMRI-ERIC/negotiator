package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Definition Resolution while every Resource runs the Global Default Family. Associating a
 * Resource with a family, directly or through a Network, changes only {@link
 * #resolveForResource()}.
 */
@Service
class DefinitionResolverImpl implements DefinitionResolver {

  private final LifecycleDefinitionRepository definitions;

  DefinitionResolverImpl(LifecycleDefinitionRepository definitions) {
    this.definitions = definitions;
  }

  @Override
  public LifecycleDefinition resolveForNegotiation() {
    List<LifecycleDefinition> active =
        definitions.findByScopeAndActiveTrue(DefinitionScope.NEGOTIATION);
    if (active.isEmpty()) {
      throw new DefinitionResolutionException(
          "No active NEGOTIATION-scope Lifecycle Definition exists");
    }
    if (active.size() > 1) {
      throw new DefinitionResolutionException(
          "Expected one Definition Family to have an active NEGOTIATION-scope Lifecycle Definition,"
              + " found "
              + active.stream().map(LifecycleDefinition::getFamilyKey).sorted().toList());
    }
    return active.getFirst();
  }

  @Override
  public LifecycleDefinition resolveForResource() {
    return definitions
        .findByScopeAndActiveTrueAndGlobalDefaultTrue(DefinitionScope.RESOURCE)
        .orElseThrow(
            () ->
                new DefinitionResolutionException(
                    "No RESOURCE-scope Definition Family has an active Lifecycle Definition marked"
                        + " as the global default"));
  }
}
