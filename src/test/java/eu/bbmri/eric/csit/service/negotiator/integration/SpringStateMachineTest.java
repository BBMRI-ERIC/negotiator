package eu.bbmri.eric.csit.service.negotiator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.test.context.ActiveProfiles;

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
  @Qualifier("testMachine")
  private StateMachine<String, String> entityStateMachine;

  @Autowired
  @Qualifier("testHandler")
  private PersistStateMachineHandler persistStateMachineHandler;

  @Test
  void init() {
    persistStateMachineHandler
        .handleEventWithStateReactively(
            MessageBuilder.withPayload(TestEvents.APPROVE.name())
                .setHeader("negotiation", "testId")
                .build(),
            TestStates.SUBMITTED.name())
        .subscribe();
  }

  @Test
  void getPossibleEvents_ok() {
    assertEquals(List.of(TestEvents.APPROVE), getPossibleEvents(TestStates.SUBMITTED));
  }

  private List<TestEvents> getPossibleEvents(TestStates testStates) {
    entityStateMachine
        .getStateMachineAccessor()
        .doWithAllRegions(
            accessor ->
                accessor.resetStateMachineReactively(
                    new DefaultStateMachineContext<>(testStates.name(), null, null, null)));
    return entityStateMachine.getTransitions().stream()
        .filter(
            transition ->
                transition.getSource().getId().equals(entityStateMachine.getState().getId()))
        .map(transition -> transition.getTrigger().getEvent())
        .map(TestEvents::valueOf)
        .collect(Collectors.toList());
  }
}
