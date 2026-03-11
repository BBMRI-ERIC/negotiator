package eu.bbmri_eric.negotiator.negotiation.state_machine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.ObjectStateMachineFactory;
import org.springframework.statemachine.config.model.StateMachineModel;

@Configuration
public class StateMachineFactory {
  @Bean(name = "custom")
  org.springframework.statemachine.config.StateMachineFactory<String, String> stateMachineFactory(
      DBStateMachineFactory modelFactory) {
    StateMachineModel<String, String> defaultModel = modelFactory.build("default");
    return new ObjectStateMachineFactory<>(defaultModel, modelFactory);
  }
}
