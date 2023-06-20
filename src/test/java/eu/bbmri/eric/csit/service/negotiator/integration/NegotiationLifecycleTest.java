package eu.bbmri.eric.csit.service.negotiator.integration;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleService;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationResourceLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NegotiationLifecycleTest {

    @Autowired
    NegotiationLifecycleService negotiationLifecycleService;

    @Autowired
    NegotiationResourceLifecycleService negotiationResourceLifecycleService;

    @Test
    void testInitializeStateMachineForNegotiation() {
        negotiationLifecycleService.initializeTheStateMachine("new-negotiation");
    }

    @Test
    void testInitializeStateMachineForNegotiationAndResources() {
        negotiationLifecycleService.initializeTheStateMachine("new-negotiation");
        negotiationResourceLifecycleService.initializeTheStateMachine("new-negotiation", "res-1");
        negotiationResourceLifecycleService.initializeTheStateMachine("new-negotiation", "res-2");
    }


}
