package eu.bbmri_eric.negotiator.negotiation.state_machine;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Temporary catalog for metadata held by {@link NegotiationState}, {@link NegotiationEvent}, {@link
 * NegotiationResourceState}, and {@link NegotiationResourceEvent}.
 *
 * <p>Delete this class at the Lifecycle cutover. {@link #nameExists} is replaced by checking for
 * the named {@code state} or {@code event} row in the applicable Definition Version; {@link
 * #metadata} and {@link #label} are replaced by reading that row's label and description; and
 * {@link #resourceStateOrdinal} is replaced by reading the Resource State row's ordering value.
 */
@Component
public class EnumBackedLifecycleCatalog {

  private static final Map<CatalogKey, Map<String, Metadata>> METADATA =
      Map.of(
          new CatalogKey(Scope.NEGOTIATION, Element.STATE),
          buildMetadata(
              NegotiationState.values(),
              NegotiationState::getLabel,
              NegotiationState::getDescription),
          new CatalogKey(Scope.NEGOTIATION, Element.EVENT),
          buildMetadata(
              NegotiationEvent.values(),
              NegotiationEvent::getLabel,
              NegotiationEvent::getDescription),
          new CatalogKey(Scope.RESOURCE, Element.STATE),
          buildMetadata(
              NegotiationResourceState.values(),
              NegotiationResourceState::getLabel,
              NegotiationResourceState::getDescription),
          new CatalogKey(Scope.RESOURCE, Element.EVENT),
          buildMetadata(
              NegotiationResourceEvent.values(),
              NegotiationResourceEvent::getLabel,
              NegotiationResourceEvent::getDescription));

  private static final Map<String, Integer> RESOURCE_STATE_ORDINALS =
      Arrays.stream(NegotiationResourceState.values())
          .collect(
              Collectors.toUnmodifiableMap(
                  NegotiationResourceState::name, NegotiationResourceState::ordinal));

  public boolean nameExists(Scope scope, Element element, String name) {
    return METADATA.get(new CatalogKey(scope, element)).containsKey(name);
  }

  public Metadata metadata(Scope scope, Element element, String name) {
    Metadata metadata = METADATA.get(new CatalogKey(scope, element)).get(name);
    if (metadata == null) {
      throw new IllegalArgumentException("Unknown %s %s: %s".formatted(scope, element, name));
    }
    return metadata;
  }

  /**
   * The human label of the named State or Event - the whole of {@link #metadata} that most callers
   * want. Exists so that a caller needing only the label does not spell the walk to it; a caller
   * needing the description as well still asks {@link #metadata} once.
   */
  public String label(Scope scope, Element element, String name) {
    return metadata(scope, element, name).label();
  }

  public int resourceStateOrdinal(String name) {
    Integer ordinal = RESOURCE_STATE_ORDINALS.get(name);
    if (ordinal == null) {
      throw new IllegalArgumentException("Unknown RESOURCE STATE: " + name);
    }
    return ordinal;
  }

  private static <E extends Enum<E>> Map<String, Metadata> buildMetadata(
      E[] values, Function<E, String> label, Function<E, String> description) {
    return Arrays.stream(values)
        .collect(
            Collectors.toUnmodifiableMap(
                Enum::name, value -> new Metadata(label.apply(value), description.apply(value))));
  }

  public enum Scope {
    NEGOTIATION,
    RESOURCE
  }

  public enum Element {
    STATE,
    EVENT
  }

  public record Metadata(String label, String description) {}

  private record CatalogKey(Scope scope, Element element) {}
}
