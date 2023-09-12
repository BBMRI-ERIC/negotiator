package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This interface provides a specification for Lifecycle management operations on a Resource level.
 */
public interface ResourceLifecycleService {

  /**
   * Returns all possible events that can be sent for this resource negotiation
   *
   * @param negotiationId that is of interest
   * @param resourceId that is of interest
   * @return a set of all possible events
   */
  Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId)
      throws EntityNotFoundException;

  /**
   * Send an event to a particular resource negotiation
   *
   * @return The new state
   * @throws NoSuchElementException In case the combination of Negotiation and Resource was not
   *     found
   */
  NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException;
}
