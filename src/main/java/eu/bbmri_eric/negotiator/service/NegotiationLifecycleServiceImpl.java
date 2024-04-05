package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.WrongRequestException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.security.SecurityRule;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

/** Spring State Machine implementation of the NegotiationLifecycleService. */
@Service
@CommonsLog
@NoArgsConstructor
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  @Autowired NegotiationRepository negotiationRepository;

  @Autowired
  @Qualifier("persistHandler")
  private PersistStateMachineHandler persistStateMachineHandler;

  @Autowired
  @Qualifier("negotiationStateMachine")
  private StateMachine<String, String> stateMachine;

  @Override
  public Set<NegotiationEvent> getPossibleEvents(String negotiationId)
      throws EntityNotFoundException {
    rehydrateStateMachineForNegotiation(negotiationId);
    return Set.of(NegotiationEvent.CONCLUDE);
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
    List<String> roles = NegotiatorUserDetailsService.getRoles();
    return stateMachine.getTransitions().stream()
        .filter(
            transition -> transition.getSource().getId().equals(stateMachine.getState().getId()))
        .filter(
            transition ->
                Optional.ofNullable(transition.getSecurityRule())
                    .map(SecurityRule::getAttributes)
                    .isPresent())
        .filter(
            transition ->
                transition.getSecurityRule().getAttributes().stream().anyMatch(roles::contains))
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
