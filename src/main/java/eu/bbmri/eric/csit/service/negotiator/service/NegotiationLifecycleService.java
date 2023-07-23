package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public interface NegotiationLifecycleService {


  /**
   * Returns the current state of a Negotiation
   *
   * @param negotiationId for which state is requested
   * @return NegotiationState
   */
  NegotiationState getCurrentState(String negotiationId) throws EntityNotFoundException;

  /**
   * Returns all possible events that can be sent for this negotiation
   *
   * @param negotiationId of the Negotiation
   * @return a lists of all possible events
   */
  Set<NegotiationEvent> getPossibleEvents(String negotiationId) throws EntityNotFoundException;

  /**
   * Send an event to a particular negotiation
   *
   * @return the new status of the Negotiation
   */
  NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException;
}
