package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.post.PostRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineException;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.stereotype.Service;

/** Spring State Machine implementation of the NegotiationLifecycleService. */
@Service
@CommonsLog
@NoArgsConstructor
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  @Autowired NegotiationRepository negotiationRepository;

  @Autowired PostRepository postRepository;

  @Autowired
  @Qualifier("persistHandler")
  private PersistStateMachineHandler persistStateMachineHandler;

  @Autowired
  @Qualifier("negotiationStateMachine")
  private StateMachine<String, String> stateMachine;

  @Override
  public Set<NegotiationEvent> getPossibleEvents(String negotiationId)
      throws EntityNotFoundException {
    return getPossibleEventsForCurrentStateMachine(negotiationId);
  }

  @Override
  public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException {
    changeStateMachine(negotiationId, negotiationEvent, null);
    return getCurrentStateForNegotiation(negotiationId);
  }

  @Override
  public NegotiationState sendEvent(
      String negotiationId, NegotiationEvent negotiationEvent, String message)
      throws WrongRequestException, EntityNotFoundException {
    changeStateMachine(negotiationId, negotiationEvent, message);
    return getCurrentStateForNegotiation(negotiationId);
  }

  private void changeStateMachine(
      String negotiationId, NegotiationEvent negotiationEvent, String message) {
    if (!getPossibleEvents(negotiationId).contains(negotiationEvent)) {
      throw new StateMachineException(
          "You are not allowed to %s the Negotiation"
              .formatted(negotiationEvent.getLabel().toLowerCase()));
    }

    persistStateMachineHandler
        .handleEventWithStateReactively(
            MessageBuilder.withPayload(negotiationEvent.name())
                .setHeader("negotiationId", negotiationId)
                .setHeader("postBody", message)
                .setHeader(
                    "postSenderId",
                    AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())
                .build(),
            getCurrentStateForNegotiation(negotiationId).name())
        .subscribe();
  }

  private NegotiationState getCurrentStateForNegotiation(String negotiationId) {
    return negotiationRepository
        .findNegotiationStateById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private Set<NegotiationEvent> getPossibleEventsForCurrentStateMachine(String negotiationId) {
    List<String> roles = AuthenticatedUserContext.getRoles();
    if (!roles.contains("ROLE_ADMIN")
        && !negotiationRepository.existsByIdAndCreatedBy_Id(
            negotiationId, AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())) {
      return Set.of();
    }
    return stateMachine.getTransitions().stream()
        .filter(
            transition ->
                transition
                    .getSource()
                    .getId()
                    .equals(getCurrentStateForNegotiation(negotiationId).toString()))
        .filter(
            transition -> {
              if (Objects.nonNull(transition.getSecurityRule())) {
                return transition.getSecurityRule().getAttributes().stream()
                    .anyMatch(roles::contains);
              }
              return true;
            })
        .map(transition -> transition.getTrigger().getEvent())
        .map(NegotiationEvent::valueOf)
        .collect(Collectors.toSet());
  }
}
