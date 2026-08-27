package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * This interface provides a specification for Lifecycle management operations on a Negotiation
 * level.
 *
 * <p>States and Events pass through this interface as names, so callers never touch a Lifecycle
 * enum. Callers validate an Event name before calling, so an unknown name gets a different error
 * than a known Event that cannot fire from the current State.
 */
@Service
public interface NegotiationLifecycleService {

  /**
   * Returns the names of all possible Lifecycle events that can be sent to this Negotiation.
   *
   * @param negotiationId of the Negotiation
   * @return a set of all possible event names
   */
  Set<String> getPossibleEvents(String negotiationId) throws EntityNotFoundException;

  /**
   * Send an event to a particular Negotiation.
   *
   * @param event the name of the Lifecycle Event to send
   * @return the name of the new state of the Negotiation
   */
  String sendEvent(String negotiationId, String event)
      throws WrongRequestException, EntityNotFoundException;

  /**
   * Send an event to a particular Negotiation also specifying a message with reason why.
   *
   * @param event the name of the Lifecycle Event to send
   * @return the name of the new state of the Negotiation
   */
  String sendEvent(String negotiationId, String event, String message)
      throws WrongRequestException, EntityNotFoundException;
}
