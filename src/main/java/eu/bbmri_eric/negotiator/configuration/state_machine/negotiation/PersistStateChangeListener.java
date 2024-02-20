package eu.bbmri_eric.negotiator.configuration.state_machine.negotiation;

import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.NotificationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired NegotiationRepository negotiationRepository;
  @Autowired PersonRepository personRepository;
  @Autowired NotificationRepository notificationRepository;

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
  }
}
