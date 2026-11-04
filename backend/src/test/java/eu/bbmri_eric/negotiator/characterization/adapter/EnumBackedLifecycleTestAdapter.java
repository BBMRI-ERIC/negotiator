package eu.bbmri_eric.negotiator.characterization.adapter;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.dto.UpdateResourcesDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleService;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The only test-scope file allowed to name the Lifecycle enums, which the forbidden-reference guard
 * enforces. Deleting the enums means rewriting this file and no assertion.
 *
 * <p>The services take names, so the enums serve only as a typo guard: a misspelled name in a test
 * fails loudly instead of looking like a refused Event. That is why each resolved constant turns
 * straight back into a name.
 */
final class EnumBackedLifecycleTestAdapter implements LifecycleTestAdapter {

  private final NegotiationLifecycleService negotiationLifecycleService;
  private final ResourceLifecycleService resourceLifecycleService;
  private final NegotiationRepository negotiationRepository;
  private final ResourceService resourceService;

  EnumBackedLifecycleTestAdapter(
      NegotiationLifecycleService negotiationLifecycleService,
      ResourceLifecycleService resourceLifecycleService,
      NegotiationRepository negotiationRepository,
      ResourceService resourceService) {
    this.negotiationLifecycleService = negotiationLifecycleService;
    this.resourceLifecycleService = resourceLifecycleService;
    this.negotiationRepository = negotiationRepository;
    this.resourceService = resourceService;
  }

  @Override
  public Set<String> possibleNegotiationEvents(String negotiationId) {
    return Set.copyOf(negotiationLifecycleService.getPossibleEvents(negotiationId));
  }

  @Override
  public String sendNegotiationEvent(String negotiationId, String event) {
    return negotiationLifecycleService.sendEvent(negotiationId, negotiationEventName(event));
  }

  @Override
  public String sendNegotiationEvent(String negotiationId, String event, String message) {
    return negotiationLifecycleService.sendEvent(
        negotiationId, negotiationEventName(event), message);
  }

  @Override
  public String currentNegotiationState(String negotiationId) {
    return negotiationRepository
        .findNegotiationStateById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  @Override
  public Set<String> possibleResourceEvents(String negotiationId, String resourceId) {
    return Set.copyOf(resourceLifecycleService.getPossibleEvents(negotiationId, resourceId));
  }

  @Override
  public String sendResourceEvent(String negotiationId, String resourceId, String event) {
    return resourceLifecycleService.sendEvent(negotiationId, resourceId, resourceEventName(event));
  }

  @Override
  public String currentResourceState(String negotiationId, String resourceId) {
    return negotiationRepository
        .findNegotiationResourceStateById(negotiationId, resourceId)
        .orElse(null);
  }

  @Override
  public Map<String, String> overrideResourceStates(
      String negotiationId, List<Long> resourceRowIds, String state) {
    List<ResourceWithStatusDTO> resources =
        resourceService.updateResourcesInANegotiation(
            negotiationId, new UpdateResourcesDTO(resourceRowIds, resourceStateName(state)));
    Map<String, String> statesBySourceId = new HashMap<>();
    for (ResourceWithStatusDTO resource : resources) {
      statesBySourceId.put(resource.getSourceId(), resource.getCurrentState());
    }
    return statesBySourceId;
  }

  @Override
  public Map<String, Object> resourceLifecycleDiagram() {
    return resourceLifecycleService.getStateMachineDiagram();
  }

  private static String negotiationEventName(String event) {
    try {
      return NegotiationEvent.valueOf(event).name();
    } catch (IllegalArgumentException | NullPointerException e) {
      throw unknownName(event, "Negotiation Event", NegotiationEvent.values());
    }
  }

  private static String resourceEventName(String event) {
    try {
      return NegotiationResourceEvent.valueOf(event).name();
    } catch (IllegalArgumentException | NullPointerException e) {
      throw unknownName(event, "Resource Event", NegotiationResourceEvent.values());
    }
  }

  private static String resourceStateName(String state) {
    try {
      return NegotiationResourceState.valueOf(state).name();
    } catch (IllegalArgumentException | NullPointerException e) {
      throw unknownName(state, "Resource State", NegotiationResourceState.values());
    }
  }

  /**
   * Fails on a misspelled name. The exception differs from every one the services throw, so a test
   * cannot mistake a typo for the behaviour it pins.
   */
  private static IllegalArgumentException unknownName(String given, String kind, Enum<?>[] known) {
    return new IllegalArgumentException(
        "'%s' is not a known %s name. Known names: %s"
            .formatted(
                given,
                kind,
                Arrays.stream(known).map(Enum::name).collect(Collectors.joining(", "))));
  }
}
