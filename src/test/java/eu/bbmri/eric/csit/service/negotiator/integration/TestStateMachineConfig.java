package eu.bbmri.eric.csit.service.negotiator.integration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Configuration
@Profile("test")
@EnableStateMachine(name = "testMachine")
public class TestStateMachineConfig extends StateMachineConfigurerAdapter<String, String> {

    @Override
    public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
        Set<String> stringStates = new HashSet<>();
        EnumSet.allOf(TestStates.class).forEach(entity -> stringStates.add(entity.name()));
        states.withStates().initial(TestStates.SUBMITTED.name()).states(stringStates);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<String, String> transitions)
            throws Exception {
        transitions
                .withExternal()
                .source(TestStates.SUBMITTED.name())
                .target(TestStates.APPROVED.name())
                .event(TestEvents.APPROVE.name());
    }
}
