package eu.bbmri_eric.negotiator.negotiation.dto;

import eu.bbmri_eric.negotiator.negotiation.state_machine.EnumBackedLifecycleCatalog;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Objects;

/**
 * Answers {@link KnownNegotiationStateNames} out of the Lifecycle catalog rather than out of a
 * hand-kept list, so that this check cannot drift from what a State actually is.
 *
 * <p>The catalog is enum-backed today and is replaced, at the cutover, by a read of the {@code
 * state} rows of the Negotiation scope's active Definition Version. Nothing here changes when that
 * happens: this class asks whether a name exists and the catalog decides how it knows.
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
