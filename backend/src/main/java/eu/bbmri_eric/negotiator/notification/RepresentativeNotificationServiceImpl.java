package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.user.Person;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class RepresentativeNotificationServiceImpl implements RepresentativeNotificationService {
  private final NegotiationRepository negotiationRepository;
  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private static final String PENDING_REQUEST_TEMPLATE = "email-negotiation-reminder";

  public RepresentativeNotificationServiceImpl(
      NegotiationRepository negotiationRepository,
      NotificationRepository notificationRepository,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.notificationRepository = notificationRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void notifyAboutPendingNegotiations() {
    log.debug("Looking for pending negotiations");
    Set<Negotiation> negotiations =
        new HashSet<>(negotiationRepository.findAllCreatedOn(LocalDateTime.now().minusDays(5)));
    for (Negotiation negotiation : negotiations) {
      Set<Person> reps = getRepresentativesToNotify(negotiation);
      notifyRepresentatives(negotiation, reps);
    }
  }

  @Override
  public void notifyAboutANewNegotiation(String negotiationId) {
    Negotiation negotiation = negotiationRepository.findById(negotiationId).orElse(null);
    if (negotiation == null) {
      log.error("Could not find Negotiation with ID: %s for sending out Notifications".formatted(negotiationId));
      return;
    }
    createNotificationsForRepresentatives(negotiation);
    markResourcesWithoutARepresentative(negotiation);
  }


  private void createNotificationsForRepresentatives(Negotiation negotiation) {
    Set<Person> representatives = getRepresentativesForNegotiation(negotiation);
    for (Person representative : representatives) {
      createNewNotification(negotiation, representative);
      Set<Resource> overlappingResources =
              getResourcesInNegotiationRepresentedBy(negotiation, representative);
      markReachableResources(negotiation, overlappingResources);
    }
  }

  private static Set<Resource> getResourcesInNegotiationRepresentedBy(
          Negotiation negotiation, Person representative) {
    Set<Resource> overlappingResources = new HashSet<>(representative.getResources());
    overlappingResources.retainAll(negotiation.getResources());
    return overlappingResources;
  }

  private static Set<Person> getRepresentativesForNegotiation(Negotiation negotiation) {
    return negotiation.getResources().stream()
            .filter(
                    resource ->
                            Objects.equals(
                                    negotiation.getCurrentStateForResource(resource.getSourceId()),
                                    NegotiationResourceState.SUBMITTED))
            .map(Resource::getRepresentatives)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
  }

  private void markResourcesWithoutARepresentative(@NonNull Negotiation negotiation) {
    for (Resource resourceWithoutRep :
            negotiation.getResources().stream()
                    .filter(resource -> resource.getRepresentatives().isEmpty())
                    .collect(Collectors.toSet())) {
      log.warn(
              "Resource with ID: %s does not have a representative."
                      .formatted(resourceWithoutRep.getSourceId()));
      negotiation.setStateForResource(
              resourceWithoutRep.getSourceId(), NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    }
  }

  private void markReachableResources(
          Negotiation negotiation, @NonNull Set<Resource> overlappingResources) {
    for (Resource resourceWithRepresentative : overlappingResources) {
      negotiation.setStateForResource(
              resourceWithRepresentative.getSourceId(),
              NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    }
  }

  private void createNewNotification(Negotiation negotiation, Person representative) {
    notificationRepository.save(
            buildNewNotification(
                    negotiation, representative, "New Negotiation %s ".formatted(negotiation.getId())));
  }

  private Notification buildNewNotification(
          Negotiation negotiation, Person representative, String message) {
    Notification newNotification =
            Notification.builder()
                    .negotiationId(negotiation.getId())
                    .recipientId(representative.getId())
                    .message(message)
                    .build();
    return newNotification;
  }

  private Set<Person> getRepresentativesToNotify(Negotiation negotiation) {
    Set<Organization> involvedOrganizations = negotiation.getOrganizations();
    Set<Person> representativesToNotify = new HashSet<>();
    for (Organization organization : involvedOrganizations) {
      Set<Resource> involvedResourcesOfOrganization =
          negotiation.getResources().stream()
              .filter(resource -> resource.getOrganization().equals(organization))
              .collect(Collectors.toSet());
      Set<Person> representatives =
          involvedResourcesOfOrganization.stream()
              .flatMap(resource -> resource.getRepresentatives().stream())
              .collect(Collectors.toSet());
      if (!hasOrganizationResponded(
          negotiation, representatives, involvedResourcesOfOrganization)) {
        log.info(
            "No response from %s for negotiation %s. Sending reminder notifications to its representatives"
                .formatted(organization.getName(), negotiation.getId()));
        representativesToNotify.addAll(representatives);
      }
    }
    return representativesToNotify;
  }

  private boolean hasOrganizationResponded(
      Negotiation negotiation,
      Set<Person> representatives,
      Set<Resource> involvedResourcesOfOrganization) {
    return organizationRepsPostedComments(negotiation, representatives)
        || organizationResourcesHadStatusUpdate(negotiation, involvedResourcesOfOrganization);
  }

  private void notifyRepresentatives(Negotiation negotiation, Set<Person> representatives) {
    List<Notification> notifications =
        representatives.stream()
            .map(
                rep ->
                    new Notification(
                        rep.getId(),
                        negotiation.getId(),
                        "Pending Request",
                        String.format(
                            "%s is waiting for your response",
                            negotiation.getCreatedBy().getName())))
            .collect(Collectors.toList());
    notificationRepository.saveAll(notifications);
    notifications.forEach(
        notification ->
            eventPublisher.publishEvent(
                new NewNotificationEvent(this, notification.getId(), PENDING_REQUEST_TEMPLATE)));
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
