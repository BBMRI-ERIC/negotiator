package eu.bbmri_eric.negotiator.notification.representative;

import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.NewNotificationEvent;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class RepresentativeNotificationServiceImpl implements RepresentativeNotificationService {
  private final NegotiationRepository negotiationRepository;
  private final PersonRepository personRepository;
  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;

  public RepresentativeNotificationServiceImpl(
      NegotiationRepository negotiationRepository,
      PersonRepository personRepository,
      NotificationRepository notificationRepository,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
    this.notificationRepository = notificationRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Scheduled(cron = "0 */1 * * * *")
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void notifyAboutPendingNegotiations() {
    Set<Negotiation> negotiations =
        negotiationRepository.findAllCreatedOn(LocalDateTime.now().minusDays(5));
    for (Negotiation negotiation : negotiations) {
      notifyUnresponsiveOrganizations(negotiation);
    }
  }

  private void notifyUnresponsiveOrganizations(Negotiation negotiation) {
    Set<Organization> involvedOrganizations = negotiation.getOrganizations();
    for (Organization organization : involvedOrganizations) {
      Set<Resource> involvedResourcesOfOrganization =
          negotiation.getResources().stream()
              .filter(resource -> resource.getOrganization().equals(organization))
              .collect(Collectors.toSet());
      Set<Person> representatives =
          negotiation.getResources().stream()
              .flatMap(resource -> resource.getRepresentatives().stream())
              .collect(Collectors.toSet());
      if (isOrganizationUnresponsive(
          negotiation, representatives, involvedResourcesOfOrganization)) {
        log.debug(
            "No response from %s for negotiation %s"
                .formatted(organization.getName(), negotiation.getId()));
        notifyRepresentatives(negotiation, representatives);
      }
    }
  }

  private boolean isOrganizationUnresponsive(
      Negotiation negotiation,
      Set<Person> representatives,
      Set<Resource> involvedResourcesOfOrganization) {
    return !organizationRepsPostedComments(negotiation, representatives)
        && !organizationResourcesHadStatusUpdate(negotiation, involvedResourcesOfOrganization);
  }

  private void notifyRepresentatives(Negotiation negotiation, Set<Person> representatives) {
    for (Person representative : representatives) {
      Notification notification =
          notificationRepository.save(
              new Notification(
                  representative,
                  negotiation,
                  "Pending Request",
                  "%s is waiting for you response".formatted(negotiation.getCreatedBy().getName()),
                  NotificationEmailStatus.EMAIL_NOT_SENT));
      eventPublisher.publishEvent(
          new NewNotificationEvent(this, notification.getId(), "negotiation-reminder"));
    }
  }

  private boolean organizationResourcesHadStatusUpdate(
      Negotiation negotiation, Set<Resource> involvedResourcesOfOrganization) {
    for (Resource resource : involvedResourcesOfOrganization) {
      if (!Objects.equals(
          negotiation.getCurrentStateForResource(resource.getSourceId()),
          NegotiationResourceState.REPRESENTATIVE_CONTACTED)) {
        return true;
      }
    }
    return false;
  }

  private boolean organizationRepsPostedComments(
      Negotiation negotiation, Set<Person> representatives) {
    for (Post post : negotiation.getPosts()) {
      if (representatives.contains(post.getCreatedBy())) {
        return true;
      }
    }
    return false;
  }
}
