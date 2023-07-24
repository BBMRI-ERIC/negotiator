package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
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
}
