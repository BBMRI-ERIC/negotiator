package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.NoSuchElementException;
import java.util.Set;

public interface NegotiationResourceLifecycleService {
  /**
   * Returns the current state of a Resource Negotiation
   *
   * @param negotiationId for which state is requested
   * @param resourceId for which state is requested
   * @return Current state
   */
  NegotiationResourceState getCurrentState(String negotiationId, String resourceId)
      throws EntityNotFoundException;

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
