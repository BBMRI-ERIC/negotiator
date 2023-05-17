package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import jdk.jfr.Name;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Bean(name = "negotiationStateMachineRuntimePersister")
    public StateMachineRuntimePersister<NegotiationState, NegotiationEvent, String> stateMachineRuntimePersister(
            JpaStateMachineRepository jpaStateMachineRepository) {
        return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository){};
    }

    @Bean(name = "negotiationStateMachineService")
    public StateMachineService<NegotiationState, NegotiationEvent> negotiationStateMachineService(
            StateMachineFactory<NegotiationState, NegotiationEvent> stateMachineFactory,
            StateMachineRuntimePersister<NegotiationState, NegotiationEvent, String> stateMachineRuntimePersister) {
        return new DefaultStateMachineService<>(stateMachineFactory, stateMachineRuntimePersister);
    }
    @Bean(name = "negotiationResourceStateMachineService")
    public StateMachineService<NegotiationState, NegotiationEvent> negotiationResourceStateMachineService(
            StateMachineFactory<NegotiationState, NegotiationEvent> negotiationResourceStateMachineFactory,
            StateMachineRuntimePersister<NegotiationState, NegotiationEvent, String> stateMachineRuntimePersister) {
        return new DefaultStateMachineService<>(negotiationResourceStateMachineFactory, stateMachineRuntimePersister);
    }
}
