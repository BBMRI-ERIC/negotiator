package eu.bbmri.eric.csit.service.negotiator.integration;

import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

enum TestStates {
  SUBMITTED,
  APPROVED,
  ABANDONED,
  CONCLUDED
}

enum TestEvents {
  APPROVE,
  ABANDON
}

@SpringBootTest
@ActiveProfiles("test")
@CommonsLog
public class SpringStateMachineTest {

    @Autowired
    @Qualifier("testHandler")
    private PersistStateMachineHandler persistStateMachineHandler;

    @Autowired
    private NegotiationRepository negotiationRepository;

  @Test
  void init() {
      long count = negotiationRepository.count();
      log.info(count);
      persistStateMachineHandler.handleEventWithStateReactively(MessageBuilder.
                      withPayload(TestEvents.APPROVE.name()).setHeader("negotiation", "testId").build(), TestStates.SUBMITTED.name()).subscribe();
      assertEquals(count + 1, negotiationRepository.count());
      log.info(negotiationRepository.count());
  }
}
