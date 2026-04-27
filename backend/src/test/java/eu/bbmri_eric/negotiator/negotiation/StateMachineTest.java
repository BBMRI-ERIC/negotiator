package eu.bbmri_eric.negotiator.negotiation;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.negotiation.state_machine.State;
import eu.bbmri_eric.negotiator.negotiation.state_machine.StateMachineRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.StateRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.StateType;
import eu.bbmri_eric.negotiator.negotiation.state_machine.Transition;
import eu.bbmri_eric.negotiator.negotiation.state_machine.TransitionRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;

@IntegrationTest
@Transactional
public class StateMachineTest {

  @Autowired private StateMachineRepository stateMachineRepository;

  @Autowired private StateRepository stateRepository;

  @Autowired private TransitionRepository transitionRepository;

  @Autowired
  @Qualifier("custom")
  private StateMachineFactory<String, String> stateMachineFactory;

  @BeforeEach
  void setUp() {
    // Clean up
    transitionRepository.deleteAll();
    stateRepository.deleteAll();
    stateMachineRepository.deleteAll();

    // Create a simple state machine
    eu.bbmri_eric.negotiator.negotiation.state_machine.StateMachine sm =
        new eu.bbmri_eric.negotiator.negotiation.state_machine.StateMachine();
    sm.setName("default");
    sm = stateMachineRepository.save(sm);
    System.out.println(sm.getId());

    // Create states: PENDING -> APPROVED -> COMPLETED
    State pending = new State();
    pending.setStateMachineId(sm.getId());
    pending.setName("PENDING");
    pending.setType(StateType.START);
    pending = stateRepository.save(pending);

    State approved = new State();
    approved.setStateMachineId(sm.getId());
    approved.setName("APPROVED");
    approved.setType(StateType.NORMAL);
    approved = stateRepository.save(approved);

    State completed = new State();
    completed.setStateMachineId(sm.getId());
    completed.setName("COMPLETED");
    completed.setType(StateType.END);
    completed = stateRepository.save(completed);

    // Create transitions
    Transition t1 = new Transition();
    t1.setSource(pending.getId());
    t1.setTarget(approved.getId());
    t1.setName("APPROVE");
    transitionRepository.save(t1);

    Transition t2 = new Transition();
    t2.setSource(approved.getId());
    t2.setTarget(completed.getId());
    t2.setName("COMPLETE");
    transitionRepository.save(t2);
  }

  @Test
  void testCreateStateMachineFromDatabaseAndTransition() {
    StateMachine<String, String> sm = startMachine("default");

    assertNotNull(sm.getState());
    assertEquals("PENDING", sm.getState().getId());
    System.out.println("Initial state: " + sm.getState().getId());

    sm.sendEvent(
            reactor.core.publisher.Mono.just(
                org.springframework.messaging.support.MessageBuilder.withPayload("APPROVE")
                    .build()))
        .blockLast();
    assertEquals("APPROVED", sm.getState().getId());
    System.out.println("After APPROVE event: " + sm.getState().getId());

    sm.sendEvent(
            reactor.core.publisher.Mono.just(
                org.springframework.messaging.support.MessageBuilder.withPayload("COMPLETE")
                    .build()))
        .blockLast();
    assertEquals("COMPLETED", sm.getState().getId());
    System.out.println("After COMPLETE event: " + sm.getState().getId());

    assertTrue(sm.isComplete());
  }

  public StateMachine<String, String> startMachine(String machineId) {
    StateMachine<String, String> sm = stateMachineFactory.getStateMachine(machineId);
    sm.startReactively().block();
    return sm;
  }
}
