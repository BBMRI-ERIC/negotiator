package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;

@Configuration
@CommonsLog
public class StateMachinePersistenceConfig {
    @Autowired
    private NegotiationRepository negotiationRepository;

    @Bean(name = "negotiationStateMachineRuntimePersister")
    public StateMachineRuntimePersister<NegotiationState, NegotiationEvent, String> stateMachineRuntimePersister(
            JpaStateMachineRepository jpaStateMachineRepository) {
        return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository){
            @Override
            public void write(StateMachineContext<NegotiationState, NegotiationEvent> context, String contextObj) throws Exception {
                super.write(context, contextObj);
                Negotiation negotiation = negotiationRepository.findById(contextObj).orElseThrow(() -> new EntityNotFoundException(contextObj));
                negotiation.setStatus(context.getState().toString());
                log.info(negotiation.toString());
                negotiationRepository.save(negotiation);
            }
        };
    }

    @Bean(name = "negotiationStateMachineService")
    public StateMachineService<NegotiationState, NegotiationEvent> stateMachineService(
            StateMachineFactory<NegotiationState, NegotiationEvent> stateMachineFactory,
            StateMachineRuntimePersister<NegotiationState, NegotiationEvent, String> stateMachineRuntimePersister) {
        return new DefaultStateMachineService<>(stateMachineFactory, stateMachineRuntimePersister);
    }
}
