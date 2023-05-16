package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public interface NegotiationStateService {

    /**
     * Initializes the state machine for the first time
     * @param negotiationId for which the state machine is created
     */

    void initializeTheStateMachine(String negotiationId);

    /**
     * Initializes the state machine for the first time for a specific resource in a Negotiation
     * @param negotiationId for which the state machine is created
     * @param resourceId for which the state machine is created
     */

    void initializeTheStateMachine(String negotiationId, String resourceId) throws IllegalArgumentException;
    /**
     * Returns the current state of a Negotiation
     * @param negotiationId for which state is requested
     * @return NegotiationState
     */
    NegotiationState getCurrentState(String negotiationId) throws IllegalArgumentException;

    /**
     * Returns the current state of a Resource Negotiation
     * @param negotiationId for which state is requested
     * @param resourceId for which state is requested
     * @return Current state
     */
    NegotiationState getCurrentState(String negotiationId, String resourceId) throws IllegalArgumentException;

    /**
     * Returns all possible events that can be sent for this negotiation
     * @param negotiationId of the Negotiation
     * @return a lists of all possible events
     */

    Set<NegotiationEvent> getPossibleEvents(String negotiationId) throws IllegalArgumentException;


    /**
     * Returns all possible events that can be sent for this resource negotiation
     * @param negotiationId that is of interest
     * @param resourceId that is of interest
     * @return a set of all possible events
     */
    Set<NegotiationEvent> getPossibleEvents(String negotiationId, String resourceId) throws IllegalArgumentException;

    /**
     * Send an event to a particular negotiation
     * @return the new status of the Negotiation
     */
    NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) throws UnsupportedOperationException, IllegalArgumentException;

    /**
     * Send an event to a particular resource negotiation
     * @return The new state
     * @throws NoSuchElementException In case the combination of Negotiation and Resource was not found
     */
    NegotiationState sendEvent(String negotiationId, String resourceId, NegotiationEvent negotiationEvent) throws UnsupportedOperationException, IllegalArgumentException;
}
