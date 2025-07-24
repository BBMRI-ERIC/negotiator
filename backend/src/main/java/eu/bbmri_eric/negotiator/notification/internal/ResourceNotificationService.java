package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;

/** Service for handling resource state management and representative notifications. */
@Service
@CommonsLog
class ResourceNotificationService {

  static final String BODY =
      "A new negotiation request requires your attention. Please review the details and respond accordingly.";
  static final String TITLE = "New Negotiation Request";

  private final NotificationService notificationService;
  private final NegotiationRepository negotiationRepository;

  ResourceNotificationService(
      NotificationService notificationService, NegotiationRepository negotiationRepository) {
    this.notificationService = notificationService;
    this.negotiationRepository = negotiationRepository;
  }

  /**
   * Handles resource state management for negotiations moving to IN_PROGRESS status. Sets initial
   * states for resources without existing states.
   */
  public void notifyResourceRepresentatives(String negotiationId) {
    handleResourceStateManagement(negotiationId);
  }

  private void handleResourceStateManagement(String negotiationId) {
    Negotiation negotiation = findNegotiation(negotiationId);
    if (!negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      return;
    }
    Set<Person> contactedRepresentatives = new HashSet<>();
    for (Resource resource : negotiation.getResources()) {
      if (Objects.nonNull(negotiation.getCurrentStateForResource(resource.getSourceId()))) {
        continue;
      }
      if (resource.getRepresentatives().isEmpty()) {
        negotiation.setStateForResource(
            resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
      } else {
        negotiation.setStateForResource(
            resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_CONTACTED);
        contactedRepresentatives.addAll(resource.getRepresentatives());
      }
    }
    notifyRepresentatives(negotiationId, contactedRepresentatives);
  }

  private Negotiation findNegotiation(String negotiationId) {
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
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
