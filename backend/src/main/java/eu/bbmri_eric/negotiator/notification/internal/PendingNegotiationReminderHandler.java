package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

/** Handles scheduled reminder notifications for pending negotiations. */
@Component
@CommonsLog
class PendingNegotiationReminderHandler
    implements NotificationStrategy<PendingNegotiationReminderEvent> {

  private final NotificationService notificationService;
  private final NegotiationRepository negotiationRepository;

  PendingNegotiationReminderHandler(
      NotificationService notificationService, NegotiationRepository negotiationRepository) {
    this.notificationService = notificationService;
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  public Class<PendingNegotiationReminderEvent> getSupportedEventType() {
    return PendingNegotiationReminderEvent.class;
  }

  @Override
  @Transactional
  public void notify(PendingNegotiationReminderEvent event) {
    log.info("Processing scheduled reminder notifications for pending negotiations");

    Set<Negotiation> pendingNegotiations =
        new HashSet<>(negotiationRepository.findAllCreatedOn(LocalDateTime.now().minusDays(5)));

    for (Negotiation negotiation : pendingNegotiations) {
      Set<Person> representatives = getRepresentativesToNotify(negotiation);
      if (!representatives.isEmpty()) {
        notifyRepresentatives(negotiation, representatives);
      }
    }

    log.info(
        "Completed scheduled reminder notifications for "
            + pendingNegotiations.size()
            + " pending negotiations");
  }

  private Set<Person> getRepresentativesToNotify(Negotiation negotiation) {
    Set<Person> representatives = new HashSet<>();

    for (Resource resource : negotiation.getResources()) {
      if (negotiation.getCurrentStateForResource(resource.getSourceId())
          == NegotiationResourceState.REPRESENTATIVE_CONTACTED) {
        representatives.addAll(resource.getRepresentatives());
      }
    }

    return representatives.stream()
        .filter(Objects::nonNull)
        .collect(java.util.stream.Collectors.toSet());
  }

  private void notifyRepresentatives(Negotiation negotiation, Set<Person> representatives) {
    List<Long> representativeIds = representatives.stream().map(Person::getId).toList();

    NotificationCreateDTO notification =
        new NotificationCreateDTO(
            representativeIds,
            "Pending Negotiation Reminder",
            "You have a pending negotiation request that requires your attention. Please review and respond to: "
                + negotiation.getTitle(),
            negotiation.getId());

    notificationService.createNotifications(notification);
    log.info(
        "Sent reminder notifications to "
            + representatives.size()
            + " representatives for negotiation: "
            + negotiation.getId());
  }
}
