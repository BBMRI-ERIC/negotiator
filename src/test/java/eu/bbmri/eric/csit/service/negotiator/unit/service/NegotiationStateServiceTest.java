package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NegotiationStateServiceTest {

    private static NegotiationStateService negotiationStateService;


    @BeforeAll
    static void beforeAll() {
        negotiationStateService = new NegotiationStateService() {
            @Override
            public NegotiationState getNegotiationState(String negotiationId) {
                return NegotiationState.SUBMITTED;
            }

            @Override
            public List<NegotiationEvent> getPossibleEvents(String negotiationId) {
                return List.of(NegotiationEvent.APPROVE);
            }

            @Override
            public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) {
                if (negotiationEvent == NegotiationEvent.APPROVE)
                    return NegotiationState.APPROVED;
                throw new UnsupportedOperationException("This is not a valid event");
            }
        };
    }

    @Test
    public void testGetNegotiationState() {
        assertEquals(NegotiationState.SUBMITTED, negotiationStateService.getNegotiationState("fakeId"));
    }

    @Test
    public void testGetPossibleEvents() {
        assertEquals(List.of(NegotiationEvent.APPROVE), negotiationStateService.getPossibleEvents("fakeId"));
    }

    @Test
    public void testSendValidEventToNegotiation() {
        var negotiationState = negotiationStateService.sendEvent("fakeId", NegotiationEvent.APPROVE);
        assertEquals(NegotiationState.APPROVED, negotiationState);
    }

    @Test
    public void testSendInvalidEventForNegotiation() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> negotiationStateService.sendEvent("fakeId", NegotiationEvent.ABANDON),
                "Expected doThing() to throw, but it didn't"
        );
    }
}
