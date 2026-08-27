package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This interface provides a specification for Lifecycle management operations on a Resource level.
 *
 * <p>States and Events cross this seam as bare names, so a caller neither imports nor reaches a
 * Lifecycle enum through it.
 */
public interface ResourceLifecycleService {

  /**
   * Returns the names of all possible events that can be sent for this resource negotiation
   *
   * @param negotiationId that is of interest
   * @param resourceId that is of interest
   * @return a set of all possible event names
   */
  Set<String> getPossibleEvents(String negotiationId, String resourceId)
      throws EntityNotFoundException;

  /**
   * Send an event to a particular resource negotiation
   *
   * @param event the name of the Lifecycle Event to send
   * @return the name of the new state
   * @throws NoSuchElementException In case the combination of Negotiation and Resource was not
   *     found
   */
  String sendEvent(String negotiationId, String resourceId, String event)
      throws WrongRequestException, EntityNotFoundException;

  /**
   * Get a tree like diagram of the state machine configuration.
   *
   * @return a nested object
   */
  public Map<String, Object> getStateMachineDiagram();
}
