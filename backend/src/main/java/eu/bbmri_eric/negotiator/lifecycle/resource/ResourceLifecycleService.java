package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import java.util.Map;
import java.util.Set;

/**
 * Flowable-backed implementation of Lifecycle management operations on a NegotiationResource.
 * Replaces {@code
 * eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService}.
 */
public interface ResourceLifecycleService {

  Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId)
      throws EntityNotFoundException;

  NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent negotiationResourceEvent)
      throws WrongRequestException, EntityNotFoundException;

  Map<String, Object> getStateMachineDiagram();
}
