package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class NegotiationResourceLifecycleServiceImpl
    implements NegotiationResourceLifecycleService {

  @Autowired NegotiationRepository negotiationRepository;

  @Autowired
  @Qualifier("resourcePersistHandler")
  private PersistStateMachineHandler persistStateMachineHandler;

  @Autowired
  @Qualifier("resourceStateMachine")
  private StateMachine<String, String> stateMachine;

  @Override
  public void initializeTheStateMachine(String negotiationId, String resourceId) {}

  @Override
  public NegotiationResourceState getCurrentState(String negotiationId, String resourceId)
      throws EntityNotFoundException {
    NegotiationResourceState currentState = getCurrentStateForResource(negotiationId, resourceId);
    if (Objects.isNull(currentState)) {
      throw new EntityNotFoundException(resourceId);
    }
    return currentState;
  }

  @Override
  public Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId)
      throws EntityNotFoundException {
    NegotiationResourceState currentState = getCurrentStateForResource(negotiationId, resourceId);
    if (Objects.isNull(currentState)) {
      throw new EntityNotFoundException(resourceId);
    }
    rehydrateStateMachineForResource(negotiationId, resourceId);
    return getPossibleEventsForCurrentStateMachine();
  }

  @Override
  public NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException {
    NegotiationResourceState currentState =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException("No such negotiation found."))
            .getCurrentStatePerResource()
            .get(resourceId);
    if (Objects.isNull(currentState)) {
      throw new WrongRequestException();
    }
    persistStateMachineHandler
        .handleEventWithStateReactively(
            MessageBuilder.withPayload(negotiationEvent.name())
                .setHeader("negotiationId", negotiationId)
                .setHeader("resourceId", resourceId)
                .build(),
            currentState.name())
        .subscribe();
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException("No such negotiation found."))
        .getCurrentStatePerResource()
        .get(resourceId);
  }

  private NegotiationResourceState getCurrentStateForResource(
      String negotiationId, String resourceId) {
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException("Negotiation not found."))
        .getCurrentStatePerResource()
        .get(resourceId);
  }

  private Set<NegotiationResourceEvent> getPossibleEventsForCurrentStateMachine() {
    return stateMachine.getTransitions().stream()
        .filter(
            transition -> transition.getSource().getId().equals(stateMachine.getState().getId()))
        .map(transition -> transition.getTrigger().getEvent())
        .map(NegotiationResourceEvent::valueOf)
        .collect(Collectors.toSet());
  }

  private void rehydrateStateMachineForResource(String negotiationId, String resourceId) {
    NegotiationResourceState currentState = getCurrentStateForResource(negotiationId, resourceId);
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
