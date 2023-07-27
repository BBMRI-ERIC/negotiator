package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

/** Spring State Machine implementation of the NegotiationLifecycleService. */
@Service
@CommonsLog
@NoArgsConstructor
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  @Autowired
  @Qualifier("persistHandler")
  private PersistStateMachineHandler persistStateMachineHandler;

  @Autowired
  @Qualifier("negotiationStateMachine")
  private StateMachine<String, String> stateMachine;

  @Autowired NegotiationRepository negotiationRepository;

  @Override
  public Set<NegotiationEvent> getPossibleEvents(String negotiationId)
      throws EntityNotFoundException {
    rehydrateStateMachineForNegotiation(negotiationId);
    return getPossibleEventsForCurrentStateMachine();
  }

  @Override
  public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException {
    changeStateMachine(negotiationId, negotiationEvent);
    return getCurrentStateForNegotiation(negotiationId);
  }

  private void changeStateMachine(String negotiationId, NegotiationEvent negotiationEvent) {
    persistStateMachineHandler
        .handleEventWithStateReactively(
            MessageBuilder.withPayload(negotiationEvent.name())
                .setHeader("negotiationId", negotiationId)
                .build(),
            getCurrentStateForNegotiation(negotiationId).name())
        .subscribe();
  }

  private NegotiationState getCurrentStateForNegotiation(String negotiationId) {
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException("Negotiation not found."))
        .getCurrentState();
  }

  private Set<NegotiationEvent> getPossibleEventsForCurrentStateMachine() {
    return stateMachine.getTransitions().stream()
        .filter(
            transition -> transition.getSource().getId().equals(stateMachine.getState().getId()))
        .map(transition -> transition.getTrigger().getEvent())
        .map(NegotiationEvent::valueOf)
        .collect(Collectors.toSet());
  }

  private void rehydrateStateMachineForNegotiation(String negotiationId) {
    NegotiationState currentState = getCurrentStateForNegotiation(negotiationId);
    stateMachine
        .getStateMachineAccessor()
        .doWithAllRegions(
            accessor ->
                accessor
                    .resetStateMachineReactively(
                        new DefaultStateMachineContext<>(currentState.name(), null, null, null))
                    .subscribe());
  }
}
