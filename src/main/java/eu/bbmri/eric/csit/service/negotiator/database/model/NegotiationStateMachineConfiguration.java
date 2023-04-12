package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigBuilder;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.config.configuration.StateMachineConfiguration;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class NegotiationStateMachineConfiguration extends EnumStateMachineConfigurerAdapter<NegotiationStates, NegotiationEvents> {

    @Override
    public void configure(StateMachineStateConfigurer<NegotiationStates, NegotiationEvents> states) throws Exception {
        states
                .withStates()
                .initial(NegotiationStates.SUBMITTED)
                .states(EnumSet.allOf(NegotiationStates.class))
                .end(NegotiationStates.CONCLUDED)
                .end(NegotiationStates.ABANDONED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<NegotiationStates, NegotiationEvents> transitions) throws Exception {
        transitions
                .withExternal().
                source(NegotiationStates.SUBMITTED).
                target(NegotiationStates.APPROVED).
                event(NegotiationEvents.APPROVE)
                .and()
                .withExternal().source(NegotiationStates.SUBMITTED).target(NegotiationStates.ABANDONED)
                .event(NegotiationEvents.ABANDON);
    }

    @Bean
    public StateMachineListener<NegotiationStates, NegotiationEvents> listener() {
        return new StateMachineListenerAdapter<>() {
            @Override
            public void stateChanged(State<NegotiationStates, NegotiationEvents> from, State<NegotiationStates, NegotiationEvents> to) {
                System.out.println("State change to " + to.getId());
            }
            };
    }
}
