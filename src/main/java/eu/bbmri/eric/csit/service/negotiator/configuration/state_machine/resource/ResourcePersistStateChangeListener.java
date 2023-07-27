package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import java.util.Objects;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired NegotiationRepository negotiationRepository;

  @Override
  public void onPersist(
      State<String, String> state,
      Message<String> message,
      Transition<String, String> transition,
      StateMachine<String, String> stateMachine) {
    String negotiationId = message.getHeaders().get("negotiationId", String.class);
    String resourceId = message.getHeaders().get("resourceId", String.class);
    Negotiation negotiation = null;
    if (Objects.nonNull(negotiationId) && Objects.nonNull(resourceId)) {
      negotiation = negotiationRepository.findById(negotiationId).orElse(null);
    }
    if (Objects.nonNull(negotiation)) {
      negotiation.setStateForResource(resourceId, NegotiationResourceState.valueOf(state.getId()));
      negotiationRepository.save(negotiation);
    }
  }
}
