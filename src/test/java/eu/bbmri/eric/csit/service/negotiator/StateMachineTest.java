package eu.bbmri.eric.csit.service.negotiator;

import lombok.extern.java.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.state.State;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
public class StateMachineTest {

    @Autowired
    private StateMachine<TestStates, TestEvents> stateMachine;

    @BeforeEach
    void beforeEach(){
        // reset state machine to initial state
        stateMachine.stopReactively().block();
        stateMachine.startReactively().block();
    }

    @Test
    public void testGetStateMachineUUID() {
        assertInstanceOf(UUID.class, stateMachine.getUuid());
    }

    @Test
    public void testStateMachineInitialState() {
        assertEquals(TestStates.SUBMITTED, stateMachine.getState().getId());
    }

    @Test
    public void testSendEventToStateMachine() {
        stateMachine.sendEvent(TestEvents.APPROVE);
        assertEquals(TestStates.APPROVED, stateMachine.getState().getId());
    }

    @Test
    public void testSendSameEventToStateMachineTwice() {
        stateMachine.sendEvent(TestEvents.APPROVE);
        stateMachine.sendEvent(TestEvents.APPROVE);
        assertEquals(TestStates.APPROVED, stateMachine.getState().getId());
    }

    @Test
    public void testSendOneValidOneInvalidEvent() {
        stateMachine.sendEvent(TestEvents.APPROVE);
        stateMachine.sendEvent(TestEvents.ABANDON);
        assertEquals(TestStates.APPROVED, stateMachine.getState().getId());
    }

    @Test
    public void testGetValidEvent() {
        assertEquals(Arrays.stream(new TestEvents[]{TestEvents.APPROVE}).toList(), stateMachine.getTransitions().stream()
                .filter(transition -> transition.getSource().getId().equals(stateMachine.getState().getId()))
                .map(transition -> transition.getTrigger().getEvent())
                .collect(Collectors.toList()));
    }
}

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

@Configuration
@EnableStateMachine
@Log
class StateMachineConfig
        extends EnumStateMachineConfigurerAdapter<TestStates, TestEvents> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<TestStates, TestEvents> config)
            throws Exception {
        config
                .withConfiguration()
                .autoStartup(true)
                .listener(listener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<TestStates, TestEvents> states)
            throws Exception {
        states
                .withStates()
                .initial(TestStates.SUBMITTED)
                .states(EnumSet.allOf(TestStates.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TestStates, TestEvents> transitions)
            throws Exception {
        transitions
                .withExternal()
                .source(TestStates.SUBMITTED).target(TestStates.APPROVED).event(TestEvents.APPROVE);
    }

    @Bean
    public StateMachineListener<TestStates, TestEvents> listener() {
        return new StateMachineListenerAdapter<TestStates, TestEvents>() {
            @Override
            public void stateChanged(State<TestStates, TestEvents> from, State<TestStates, TestEvents> to) {
                log.info("State change to " + to.getId());
            }
        };
    }

}

