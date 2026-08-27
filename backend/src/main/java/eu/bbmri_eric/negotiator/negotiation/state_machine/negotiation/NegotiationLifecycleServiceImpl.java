package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.EnumBackedLifecycleCatalog;
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
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.stereotype.Service;

/** Spring State Machine implementation of the NegotiationLifecycleService. */
@Service
@CommonsLog
@NoArgsConstructor
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  @Autowired NegotiationRepository negotiationRepository;

  @Autowired private EnumBackedLifecycleCatalog lifecycleCatalog;

  @Autowired
  @Qualifier("persistHandler")
  private PersistStateMachineHandler persistStateMachineHandler;

  @Autowired
  @Qualifier("negotiationStateMachine")
  private StateMachine<String, String> stateMachine;

  @Override
  public Set<String> getPossibleEvents(String negotiationId) throws EntityNotFoundException {
    return getPossibleEventsForCurrentStateMachine(negotiationId);
  }

  @Override
  public String sendEvent(String negotiationId, String event)
      throws WrongRequestException, EntityNotFoundException {
    changeStateMachine(negotiationId, event, null);
    return getCurrentStateForNegotiation(negotiationId);
  }

  @Override
  public String sendEvent(String negotiationId, String event, String message)
      throws WrongRequestException, EntityNotFoundException {
    changeStateMachine(negotiationId, event, message);
    return getCurrentStateForNegotiation(negotiationId);
  }

  private void changeStateMachine(String negotiationId, String event, String message) {
    if (!getPossibleEvents(negotiationId).contains(event)) {
      throw new ForbiddenRequestException(
          "You are not allowed to %s the Negotiation".formatted(eventLabel(event).toLowerCase()));
    }

    persistStateMachineHandler
        .handleEventWithStateReactively(
            MessageBuilder.withPayload(event)
                .setHeader("negotiationId", negotiationId)
                .setHeader("postBody", message)
                .setHeader(
                    "postSenderId",
                    AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())
                .build(),
            getCurrentStateForNegotiation(negotiationId))
        .subscribe();
  }

  /**
   * Reads a Negotiation Event's human label off the catalog rather than off the enum this service
   * is about to lose. Replaced by the label on the named {@code event} row at the Lifecycle
   * cutover.
   */
  private String eventLabel(String event) {
    return lifecycleCatalog
        .metadata(
            EnumBackedLifecycleCatalog.Scope.NEGOTIATION,
            EnumBackedLifecycleCatalog.Element.EVENT,
            event)
        .label();
  }

  private String getCurrentStateForNegotiation(String negotiationId) {
    return negotiationRepository
        .findNegotiationStateById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private Set<String> getPossibleEventsForCurrentStateMachine(String negotiationId) {
    Long userId;
    try {
      userId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    } catch (ClassCastException e) {
      throw new ForbiddenRequestException("You are not allowed to perform this action");
    }
    List<String> roles = AuthenticatedUserContext.getRoles();
    if (!roles.contains("ROLE_ADMIN")
        && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userId)) {
      return Set.of();
    }
    return stateMachine.getTransitions().stream()
        .filter(
            transition ->
                transition.getSource().getId().equals(getCurrentStateForNegotiation(negotiationId)))
        .filter(
            transition -> {
              if (Objects.nonNull(transition.getSecurityRule())) {
                return transition.getSecurityRule().getAttributes().stream()
                    .anyMatch(roles::contains);
              }
              return true;
            })
        .map(transition -> transition.getTrigger().getEvent())
        .collect(Collectors.toSet());
  }
}
