package eu.bbmri.eric.csit.service.negotiator.integration;

import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@ActiveProfiles("test")
public class NegotiationLifecycleTest {

    @Autowired
    NegotiationLifecycleService negotiationLifecycleService;

    @Autowired
    private JpaStateMachineRepository jpaStateMachineRepository;


    @Test
    void testInitializeStateMachineForNegotiation() {
        negotiationLifecycleService.initializeTheStateMachine("new-negotiation");
    }


}



