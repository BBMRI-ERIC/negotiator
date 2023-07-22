package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.Set;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.stereotype.Service;

/** Spring State Machine implementation of the NegotiationLifecycleService */
@Service
@CommonsLog
@NoArgsConstructor
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  @Autowired
  @Qualifier("persistHandler")
  private PersistStateMachineHandler persistStateMachineHandler;

  @Override
  public void initializeTheStateMachine(String negotiationId) {
    persistStateMachineHandler
        .handleEventWithStateReactively(
            MessageBuilder.withPayload(NegotiationEvent.DECLINE.name())
                .setHeader("negotiation", "testId")
                .build(),
            NegotiationState.SUBMITTED.name())
        .subscribe();
  }

  @Override
  public NegotiationState getCurrentState(String negotiationId) throws EntityNotFoundException {
    return null;
  }

  @Override
  public Set<NegotiationEvent> getPossibleEvents(String negotiationId)
      throws EntityNotFoundException {
    return null;
  }

  @Override
  public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException {
    return null;
  }
}
