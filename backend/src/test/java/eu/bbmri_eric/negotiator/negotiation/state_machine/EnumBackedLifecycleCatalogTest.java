package eu.bbmri_eric.negotiator.negotiation.state_machine;

import static eu.bbmri_eric.negotiator.negotiation.state_machine.EnumBackedLifecycleCatalog.Element.EVENT;
import static eu.bbmri_eric.negotiator.negotiation.state_machine.EnumBackedLifecycleCatalog.Element.STATE;
import static eu.bbmri_eric.negotiator.negotiation.state_machine.EnumBackedLifecycleCatalog.Scope.NEGOTIATION;
import static eu.bbmri_eric.negotiator.negotiation.state_machine.EnumBackedLifecycleCatalog.Scope.RESOURCE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EnumBackedLifecycleCatalogTest {

  private final EnumBackedLifecycleCatalog catalog = new EnumBackedLifecycleCatalog();

  @ParameterizedTest
  @MethodSource("knownNames")
  void recognizesNamesInEachDefinitionScope(
      EnumBackedLifecycleCatalog.Scope scope,
      EnumBackedLifecycleCatalog.Element element,
      String name) {
    assertThat(catalog.nameExists(scope, element, name)).isTrue();
  }

  @ParameterizedTest
  @MethodSource("scopeAndElements")
  void rejectsUnknownNames(
      EnumBackedLifecycleCatalog.Scope scope, EnumBackedLifecycleCatalog.Element element) {
    assertThat(catalog.nameExists(scope, element, "UNKNOWN")).isFalse();
  }

  @ParameterizedTest
  @MethodSource("metadata")
  void suppliesLabelsAndDescriptions(
      EnumBackedLifecycleCatalog.Scope scope,
      EnumBackedLifecycleCatalog.Element element,
      String name,
      String label,
      String description) {
    assertThat(catalog.metadata(scope, element, name))
        .isEqualTo(new EnumBackedLifecycleCatalog.Metadata(label, description));
  }

  @Test
  void suppliesResourceStateOrdinal() {
    assertThat(catalog.resourceStateOrdinal("RESOURCE_MADE_AVAILABLE")).isEqualTo(11);
  }

  private static Stream<Arguments> knownNames() {
    return Stream.of(
        Arguments.of(NEGOTIATION, STATE, "SUBMITTED"),
        Arguments.of(NEGOTIATION, EVENT, "APPROVE"),
        Arguments.of(RESOURCE, STATE, "REPRESENTATIVE_CONTACTED"),
        Arguments.of(RESOURCE, EVENT, "CONTACT"));
  }

  private static Stream<Arguments> scopeAndElements() {
    return Stream.of(
        Arguments.of(NEGOTIATION, STATE),
        Arguments.of(NEGOTIATION, EVENT),
        Arguments.of(RESOURCE, STATE),
        Arguments.of(RESOURCE, EVENT));
  }

  private static Stream<Arguments> metadata() {
    return Stream.of(
        Arguments.of(
            NEGOTIATION,
            STATE,
            "SUBMITTED",
            "Under review",
            "The negotiation has been submitted for review"),
        Arguments.of(NEGOTIATION, EVENT, "APPROVE", "Approve", "Approve the negotiation"),
        Arguments.of(
            RESOURCE,
            STATE,
            "REPRESENTATIVE_CONTACTED",
            "Representative Contacted",
            "Representative of the given resource was notified about this new request"),
        Arguments.of(RESOURCE, EVENT, "CONTACT", "Contact", "Contact representatives"));
  }
}
