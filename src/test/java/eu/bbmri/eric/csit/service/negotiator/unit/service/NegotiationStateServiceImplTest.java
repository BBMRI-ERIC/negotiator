package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class NegotiationStateServiceImplTest {

    @Autowired
    NegotiationStateServiceImpl negotiationStateService;


    @Test
    public void getStateForAFakeNegotiation() {
        assertEquals(NegotiationState.SUBMITTED, negotiationStateService.getNegotiationState("fake"));
    }

    @Test
    void getPossibleEventsForANewFakeNegotiation() {
        assertEquals(Arrays.stream(new NegotiationEvent[]{NegotiationEvent.APPROVE, NegotiationEvent.DECLINE}).toList(), negotiationStateService.getPossibleEvents("fake"));
    }

    @Test
    void sendValidApproveEventToNegotiation() {
        assertEquals(NegotiationState.APPROVED, negotiationStateService.sendEvent("negotiation-1", NegotiationEvent.APPROVE));
    }

    @Test
    void sendInvalidEventToNegotiation() {
        assertEquals(NegotiationState.SUBMITTED, negotiationStateService.sendEvent("negotiation-2", NegotiationEvent.CONCLUDE));
    }
}
