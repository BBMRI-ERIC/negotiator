package eu.bbmri.eric.csit.service.negotiator.integration;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;

@Configuration
@CommonsLog
public class PersistHandlerConfig {

    @Autowired
    @Qualifier("testMachine")
    private StateMachine<String, String> entityStateMachine;

    @Autowired
    @Qualifier("testListener")
    private LocalPersistStateChangeListener localPersistStateChangeListener;

    @Bean(name = "testHandler")
    public PersistStateMachineHandler persistStateMachineHandler() {
        PersistStateMachineHandler handler = new PersistStateMachineHandler(entityStateMachine);
        handler.addPersistStateChangeListener(localPersistStateChangeListener);
        return handler;
    }
}
