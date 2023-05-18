package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;



@Configuration
@EnableStateMachineFactory
@Log
public class NegotiationStateMachineConfig extends EnumStateMachineConfigurerAdapter<NegotiationState, NegotiationEvent> {

    @Autowired
    @Qualifier("negotiationStateMachineRuntimePersister")
    private StateMachineRuntimePersister<NegotiationState, NegotiationEvent, String> stateMachineRuntimePersister;

    @Override
    public void configure(StateMachineConfigurationConfigurer<NegotiationState, NegotiationEvent> config)
            throws Exception {
        config.withPersistence().runtimePersister(stateMachineRuntimePersister).and()
                .withConfiguration()
                .autoStartup(true)
                .listener(listener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<NegotiationState, NegotiationEvent> states)
            throws Exception {
        states
                .withStates()
                .initial(NegotiationState.SUBMITTED)
                .states(EnumSet.allOf(NegotiationState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<NegotiationState, NegotiationEvent> transitions)
            throws Exception {
        transitions
                .withExternal()
                .source(NegotiationState.SUBMITTED).target(NegotiationState.APPROVED).event(NegotiationEvent.APPROVE)
                .and().withExternal()
                .source(NegotiationState.SUBMITTED).target(NegotiationState.DECLINED).event(NegotiationEvent.DECLINE)
                .and().withExternal()
                .source(NegotiationState.APPROVED).target(NegotiationState.CONCLUDED).event(NegotiationEvent.CONCLUDE)
        ;
    }

    @Bean
    public StateMachineListener<NegotiationState, NegotiationEvent> listener() {
        return new StateMachineListenerAdapter<>() {
            @Override
            public void stateChanged(State<NegotiationState, NegotiationEvent> from, State<NegotiationState, NegotiationEvent> to) {
                log.info("State change to " + to.getId());
            }
        };
    }
}
