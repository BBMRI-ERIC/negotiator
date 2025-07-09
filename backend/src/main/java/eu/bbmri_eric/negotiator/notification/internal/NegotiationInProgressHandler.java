package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
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

  private final NotificationService notificationService;
  private final NegotiationRepository negotiationRepository;
  private final ResourceLifecycleService resourceService;
  private final AuthenticatedUserContext authenticatedUserContext;

  NegotiationInProgressHandler(
      NotificationService notificationService,
      NegotiationRepository negotiationRepository,
      ResourceLifecycleService resourceService,
      AuthenticatedUserContext authenticatedUserContext) {
    this.notificationService = notificationService;
    this.negotiationRepository = negotiationRepository;
    this.resourceService = resourceService;
    this.authenticatedUserContext = authenticatedUserContext;
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
        updateResourceState(
            negotiationId, resource.getSourceId(), NegotiationResourceEvent.MARK_AS_UNREACHABLE);
      } else {
        updateResourceState(
            negotiationId, resource.getSourceId(), NegotiationResourceEvent.CONTACT);
        contactedRepresentatives.addAll(resource.getRepresentatives());
      }
    }
    if (!contactedRepresentatives.isEmpty()) {
      notifyRepresentatives(negotiationId, contactedRepresentatives);
    }
  }

  private void updateResourceState(
      String negotiationId, String resourceId, NegotiationResourceEvent event) {
    try {
      authenticatedUserContext.runAsSystemUser(
          () -> resourceService.sendEvent(negotiationId, resourceId, event));
    } catch (Exception e) {
      log.error(
          "Failed to update resource state for resource "
              + resourceId
              + " in negotiation "
              + negotiationId
              + ": "
              + e.getMessage());
    }
  }

  private void notifyRepresentatives(String negotiationId, Set<Person> representatives) {
    List<Long> representativeIds =
        representatives.stream().filter(Objects::nonNull).map(Person::getId).toList();

    if (!representativeIds.isEmpty()) {
      NotificationCreateDTO notification =
          new NotificationCreateDTO(
              representativeIds,
              "New Negotiation Request",
              "A new negotiation request requires your attention. Please review the details and respond accordingly.",
              negotiationId);

      notificationService.createNotifications(notification);
      log.info(
          "Notified "
              + representatives.size()
              + " representatives about new negotiation: "
              + negotiationId);
    }
  }
}
