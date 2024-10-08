package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

/** Listener for changes in the Negotiation lifecycle state machine. */
@Service("persistListener")
@CommonsLog
public class PersistStateChangeListener
    implements PersistStateMachineHandler.PersistStateChangeListener {

  NegotiationRepository negotiationRepository;
  PersonRepository personRepository;
  ApplicationEventPublisher eventPublisher;

  public PersistStateChangeListener(
      NegotiationRepository negotiationRepository,
      PersonRepository personRepository,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void onPersist(
      State<String, String> state,
      Message<String> message,
      Transition<String, String> transition,
      StateMachine<String, String> stateMachine) {
    String negotiationId = message.getHeaders().get("negotiationId", String.class);
    Negotiation negotiation = null;
    if (Objects.nonNull(negotiationId)) {
      negotiation = negotiationRepository.findDetailedById(negotiationId).orElse(null);
    }
    if (Objects.nonNull(negotiation)) {
      negotiation.setCurrentState(NegotiationState.valueOf(state.getId()));
      negotiationRepository.save(negotiation);
    }
    NegotiationEvent event;
    try {
      event = NegotiationEvent.valueOf(transition.getTrigger().getEvent());
    } catch (IllegalArgumentException e) {
      log.error("Error publishing event about Negotiation status change", e);
      return;
    }
    eventPublisher.publishEvent(
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.valueOf(state.getId()), event));
  }
}
