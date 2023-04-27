package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NegotiationStateService {
    /**
     * Returns the current state of a Negotiation
     * @param negotiationId for which state is requested
     * @return NegotiationState
     */
    NegotiationState getNegotiationState(String negotiationId);

    /**
     * Returns all possible events that can be sent for this negotiation
     * @param negotiationId of the Negotiation
     * @return a lists of all possible events
     */

    List<NegotiationEvent> getPossibleEvents(String negotiationId);

    /**
     * Send an event to a particular negotiation
     * @param negotiationId of the Negotiation
     * @param negotiationEvent event being sent to the Negotiation
     * @return the new status of the Negotiation
     */

    NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent);
}
