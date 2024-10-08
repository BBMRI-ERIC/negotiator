package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import java.util.Map;
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

  /**
   * Get a tree like diagram of the state machine configuration.
   *
   * @return a nested object
   */
  public Map<String, Object> getStateMachineDiagram();
}
