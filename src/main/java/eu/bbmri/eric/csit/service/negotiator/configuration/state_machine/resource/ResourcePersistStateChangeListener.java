package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NotificationRepository;
import eu.bbmri.eric.csit.service.negotiator.service.UserNotificationService;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NotificationRepository notificationRepository;
  @Autowired @Lazy UserNotificationService userNotificationService;

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
    if (negotiation.isPresent()) {
      negotiation = updateStateForResource(state, negotiation.get(), resourceId);
      notifyRequester(negotiation.get(), resourceId);
    }
  }

  @Nullable
  private static String parseNegotiationIdFromMessage(Message<String> message) {
    return message.getHeaders().get("negotiationId", String.class);
  }

  @Nullable
  private static String parseResourceIdFromMessage(Message<String> message) {
    return message.getHeaders().get("resourceId", String.class);
  }

  private void notifyRequester(Negotiation negotiation, String resourceId) {
    userNotificationService.notifyRequesterAboutStatusChange(
        negotiation,
        negotiation.getResources().stream()
            .filter(resource -> resource.getSourceId().equals(resourceId))
            .findFirst()
            .orElse(null));
  }

  @NonNull
  private Optional<Negotiation> updateStateForResource(
      State<String, String> state, Negotiation negotiation, String resourceId) {
    negotiation.setStateForResource(resourceId, NegotiationResourceState.valueOf(state.getId()));
    return Optional.of(negotiationRepository.save(negotiation));
  }

  private Optional<Negotiation> getNegotiation(String negotiationId) {
    if (Objects.nonNull(negotiationId)) {
      return negotiationRepository.findById(negotiationId);
    }
    return Optional.empty();
  }
}
