package eu.bbmri_eric.negotiator.negotiation.dto;

import eu.bbmri_eric.negotiator.negotiation.state_machine.EnumBackedLifecycleCatalog;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Objects;

/**
 * Checks {@link KnownNegotiationStateNames} against the Lifecycle catalog rather than a hand-kept
 * list, so the check cannot drift from the States that exist.
 */
class KnownNegotiationStateNamesValidator
    implements ConstraintValidator<KnownNegotiationStateNames, Collection<String>> {

  private final EnumBackedLifecycleCatalog catalog;

  KnownNegotiationStateNamesValidator(EnumBackedLifecycleCatalog catalog) {
    this.catalog = catalog;
  }

  @Override
  public boolean isValid(Collection<String> names, ConstraintValidatorContext context) {
    if (Objects.isNull(names)) {
      return true;
    }
    return names.stream().allMatch(this::isKnown);
  }

  private boolean isKnown(String name) {
    return Objects.isNull(name)
        || catalog.nameExists(
            EnumBackedLifecycleCatalog.Scope.NEGOTIATION,
            EnumBackedLifecycleCatalog.Element.STATE,
            name);
  }
}
