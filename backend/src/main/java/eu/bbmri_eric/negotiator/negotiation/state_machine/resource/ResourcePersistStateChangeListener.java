package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.OldNotificationService;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

/** Listener for changes to the Resource State Machine. */
@Service("resourcePersistListener")
@CommonsLog
public class ResourcePersistStateChangeListener
    implements PersistStateMachineHandler.PersistStateChangeListener {

  private final NegotiationRepository negotiationRepository;
  @Lazy private final OldNotificationService oldNotificationService;
  private final ApplicationEventPublisher eventPublisher;

  public ResourcePersistStateChangeListener(
      NegotiationRepository negotiationRepository,
      OldNotificationService oldNotificationService,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.oldNotificationService = oldNotificationService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void onPersist(
      State<String, String> state,
      Message<String> message,
      Transition<String, String> transition,
      StateMachine<String, String> stateMachine) {
    String negotiationId = parseNegotiationIdFromMessage(message);
    String resourceId = parseResourceIdFromMessage(message);
    Optional<Negotiation> negotiation = getNegotiation(negotiationId);
    negotiation.ifPresent(value -> updateStateForResource(state, value, resourceId));
  }

  @Nullable
  private static String parseNegotiationIdFromMessage(Message<String> message) {
    return message.getHeaders().get("negotiationId", String.class);
  }

  @Nullable
  private static String parseResourceIdFromMessage(Message<String> message) {
    return message.getHeaders().get("resourceId", String.class);
  }

  @NonNull
  private Optional<Negotiation> updateStateForResource(
      State<String, String> state, Negotiation negotiation, String resourceId) {
    NegotiationResourceState fromState = negotiation.getCurrentStateForResource(resourceId);
    negotiation.setStateForResource(resourceId, NegotiationResourceState.valueOf(state.getId()));
    NegotiationResourceState toState = negotiation.getCurrentStateForResource(resourceId);
    eventPublisher.publishEvent(
        new ResourceStateChangeEvent(this, negotiation.getId(), resourceId, fromState, toState));
    return Optional.of(negotiationRepository.save(negotiation));
  }

  private Optional<Negotiation> getNegotiation(String negotiationId) {
    if (Objects.nonNull(negotiationId)) {
      return negotiationRepository.findById(negotiationId);
    }
    return Optional.empty();
  }
}
