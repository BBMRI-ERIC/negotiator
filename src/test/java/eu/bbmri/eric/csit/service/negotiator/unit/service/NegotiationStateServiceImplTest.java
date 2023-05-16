package eu.bbmri.eric.csit.service.negotiator.unit.service;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationStateServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class NegotiationStateServiceImplTest {

    @Autowired
    NegotiationStateServiceImpl negotiationStateService;

    @Autowired
    JpaStateMachineRepository jpaStateMachineRepository;

    final NegotiationState INITIAL_STATE = NegotiationState.SUBMITTED;
    final NegotiationState SECOND_STATE = NegotiationState.APPROVED;

    final NegotiationEvent TRANSITION_EVENT = NegotiationEvent.APPROVE;

    final String NEGOTIATION_ID = "negotiationID-1";

    @AfterEach
    void tearDown() {
        negotiationStateService.removeStateMachine(NEGOTIATION_ID);
    }

    @Test
    void getStateForNonExistentNegotiationThrowsIllegalArgException() {
        negotiationStateService.initializeTheStateMachine(NEGOTIATION_ID);
        assertThrows(
                IllegalArgumentException.class,
                () -> negotiationStateService.getCurrentState("fake")
        );
    }

    @Test
    public void getStateReturnsInitialValueAfterInitializingStateMachine() {
        negotiationStateService.initializeTheStateMachine(NEGOTIATION_ID);
        assertEquals(INITIAL_STATE, negotiationStateService.getCurrentState(NEGOTIATION_ID));
    }

    @Test
    public void getPossibleEventsForExistingNegotiation() {
        negotiationStateService.initializeTheStateMachine(NEGOTIATION_ID);
        assertEquals(INITIAL_STATE, negotiationStateService.getCurrentState(NEGOTIATION_ID));
        assertEquals(Set.of(NegotiationEvent.APPROVE, NegotiationEvent.DECLINE), negotiationStateService.getPossibleEvents(NEGOTIATION_ID));
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
