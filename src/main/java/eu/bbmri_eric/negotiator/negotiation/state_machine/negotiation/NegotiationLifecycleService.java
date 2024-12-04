package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * This interface provides a specification for Lifecycle management operations on a Negotiation
 * level.
 */
@Service
public interface NegotiationLifecycleService {

  /**
   * Returns all possible Lifecycle events that can be sent to this Negotiation.
   *
   * @param negotiationId of the Negotiation
   * @return a lists of all possible events
   */
  Set<NegotiationEvent> getPossibleEvents(String negotiationId) throws EntityNotFoundException;

  /**
   * Send an event to a particular Negotiation.
   *
   * @return the new status of the Negotiation
   */
  NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException;

  /**
   * Send an event to a particular Negotiation also specifying a message with reason why.
   *
   * @return the new status of the Negotiation
   */
  NegotiationState sendEvent(
      String negotiationId, NegotiationEvent negotiationEvent, String message)
      throws WrongRequestException, EntityNotFoundException;
}
