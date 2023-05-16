package eu.bbmri.eric.csit.service.negotiator.unit.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class NegotiationStateServiceTest {

    private static NegotiationStateService negotiationStateService;

    final NegotiationState INITIAL_STATE = NegotiationState.SUBMITTED;
    final NegotiationState SECOND_STATE = NegotiationState.APPROVED;

    final NegotiationEvent TRANSITION_EVENT = NegotiationEvent.APPROVE;

    @BeforeEach
    void beforeEach() {
        negotiationStateService = new NegotiationStateService() {

            final Map<String, NegotiationState> negotiations = new HashMap<>();

            final Map<String, NegotiationState> resourceNegotiations = new HashMap<>();



            @Override
            public void initializeTheStateMachine(String negotiationId) {
                negotiations.put(negotiationId, INITIAL_STATE);
            }

            @Override
            public void initializeTheStateMachine(String negotiationId, String resourceId) {
                resourceNegotiations.put(negotiationId + "---" + resourceId, INITIAL_STATE);
            }

            @Override
            public NegotiationState getCurrentState(String negotiationId) {
                if (negotiations.get(negotiationId) != null) {
                    return negotiations.get(negotiationId);
                }
                else throw new IllegalArgumentException("Negotiation does not exist");
            }

            @Override
            public NegotiationState getCurrentState(String negotiationId, String resourceId) {
                return null;
            }

            @Override
            public Set<NegotiationEvent> getPossibleEvents(String negotiationId) {
                if (negotiations.get(negotiationId) != null) {
                    return Set.of(TRANSITION_EVENT);
                }
                else throw new IllegalArgumentException("Negotiation does not exist");
            }

            @Override
            public Set<NegotiationEvent> getPossibleEvents(String negotiationId, String resourceId) {
                if (negotiations.get(negotiationId + "---" + resourceId) != null) {
                    return Set.of(TRANSITION_EVENT);
                }
                else throw new IllegalArgumentException("Negotiation does not exist");
            }

            @Override
            public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) {
                if (negotiations.get(negotiationId) != null && negotiationEvent == TRANSITION_EVENT) {
                    negotiations.put(negotiationId, SECOND_STATE);
                    return negotiations.get(negotiationId);
                } else if (negotiationEvent != TRANSITION_EVENT) {
                    throw new UnsupportedOperationException("Unsupported event");
                } else throw new IllegalArgumentException("Negotiation does not exist");
            }

            @Override
            public NegotiationState sendEvent(String negotiationId, String resourceId, NegotiationEvent negotiationEvent) {
                return null;
            }
        };
    }

    @Test
    void getStateForNonExistentNegotiationThrowsIllegalArgException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> negotiationStateService.getCurrentState("fakeId")
        );
    }

    @Test
    public void getStateReturnsInitialValueAfterInitializingStateMachine() {
        negotiationStateService.initializeTheStateMachine("negotiationID-1");
        assertEquals(INITIAL_STATE, negotiationStateService.getCurrentState("negotiationID-1"));
    }

    @Test
    public void getPossibleEventsForExistingNegotiation() {
        negotiationStateService.initializeTheStateMachine("negotiationID-1");
        assertEquals(Set.of(NegotiationEvent.APPROVE), negotiationStateService.getPossibleEvents("negotiationID-1"));
    }

    @Test
    public void getPossibleEventsForNonExistingNegotiation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> negotiationStateService.getPossibleEvents("fakeId")
        );
    }

    @Test
    public void sendValidEventReturnsNewStateAndIsEqualToTheCurrentState() {
        String negotiationID = "negotiationID-1";
        negotiationStateService.initializeTheStateMachine(negotiationID);
        assertEquals(SECOND_STATE, negotiationStateService.sendEvent(negotiationID, TRANSITION_EVENT));
        assertEquals(SECOND_STATE, negotiationStateService.getCurrentState(negotiationID));
    }

    @Test
    public void sendEventForNonExistentNegotiationThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> negotiationStateService.sendEvent("fakeId", TRANSITION_EVENT)
        );
    }

    @Test
    void getStateForResourceIsIndependentFromTheWholeNegotiation() {
        String negotiationID = "negotiationID-1";
        negotiationStateService.initializeTheStateMachine(negotiationID);
        negotiationStateService.initializeTheStateMachine(negotiationID, "res1");
        negotiationStateService.initializeTheStateMachine(negotiationID, "res2");
        negotiationStateService.sendEvent(negotiationID, TRANSITION_EVENT);
        assertNotEquals(negotiationStateService.getCurrentState(negotiationID), negotiationStateService.getCurrentState(negotiationID, "res1"));
    }

    @Test
    public void testSendInvalidEventForNegotiation() {
        String negotiationID = "negotiationID-1";
        negotiationStateService.initializeTheStateMachine(negotiationID);
        assertThrows(
                UnsupportedOperationException.class,
                () -> negotiationStateService.sendEvent(negotiationID, NegotiationEvent.ABANDON)
        );
    }
}
