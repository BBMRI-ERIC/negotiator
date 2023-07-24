package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;

@Configuration
@CommonsLog
public class StateMachinePersistenceConfig {

  @Bean(name = "negotiationResourceStateMachineRuntimePersister")
  public StateMachineRuntimePersister<NegotiationResourceState, NegotiationResourceEvent, String>
      negotiationResourcestateMachineRuntimePersister(
          JpaStateMachineRepository jpaStateMachineRepository) {
    return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository) {};
  }

  @Bean(name = "negotiationResourceStateMachineService")
  public StateMachineService<NegotiationResourceState, NegotiationResourceEvent>
      negotiationResourceStateMachineService(
          StateMachineFactory<NegotiationResourceState, NegotiationResourceEvent>
              negotiationResourceStateMachineFactory,
          @Qualifier("negotiationResourceStateMachineRuntimePersister")
              StateMachineRuntimePersister<
                      NegotiationResourceState, NegotiationResourceEvent, String>
                  negotiationResourcestateMachineRuntimePersister) {
    return new DefaultStateMachineService<>(
        negotiationResourceStateMachineFactory, negotiationResourcestateMachineRuntimePersister);
  }
}
