package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

/**
 * Handles negotiations moving to in-progress status by updating resource states and notifying
 * representatives.
 */
@Component
@CommonsLog
class NegotiationInProgressHandler implements NotificationStrategy<NegotiationStateChangeEvent> {

  public static final String BODY =
      "A new negotiation request requires your attention. Please review the details and respond accordingly.";
  public static final String TITLE = "New Negotiation Request";
  private final NotificationService notificationService;
  private final NegotiationRepository negotiationRepository;

  NegotiationInProgressHandler(
      NotificationService notificationService, NegotiationRepository negotiationRepository) {
    this.notificationService = notificationService;
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  public Class<NegotiationStateChangeEvent> getSupportedEventType() {
    return NegotiationStateChangeEvent.class;
  }

  @Override
  @Transactional
  public void notify(NegotiationStateChangeEvent event) {
    if (event.getChangedTo() == NegotiationState.IN_PROGRESS) {
      handleNegotiationInProgress(event.getNegotiationId());
    }
  }

  private void handleNegotiationInProgress(String negotiationId) {
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    Set<Person> contactedRepresentatives = new HashSet<>();
    for (Resource resource : negotiation.getResources()) {
      if (resource.getRepresentatives().isEmpty()) {
        negotiation.setStateForResource(
            resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
      } else {
        negotiation.setStateForResource(
            resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_CONTACTED);
        contactedRepresentatives.addAll(resource.getRepresentatives());
      }
    }
    if (!contactedRepresentatives.isEmpty()) {
      notifyRepresentatives(negotiationId, contactedRepresentatives);
    }
  }

  private void notifyRepresentatives(String negotiationId, Set<Person> representatives) {
    List<Long> representativeIds =
        representatives.stream().filter(Objects::nonNull).map(Person::getId).toList();

    if (!representativeIds.isEmpty()) {
      NotificationCreateDTO notification =
          new NotificationCreateDTO(representativeIds, TITLE, BODY, negotiationId);

      notificationService.createNotifications(notification);
      log.info(
          "Notified "
              + representatives.size()
              + " representatives about new negotiation: "
              + negotiationId);
    }
  }
}
