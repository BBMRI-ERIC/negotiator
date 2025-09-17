package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@CommonsLog
@Component
class NewPostHandler implements NotificationStrategy<NewPostEvent> {
  public static final String TITLE = "New Post Notification";
  public static final String BODY = "A new message was posted in negotiation ";
  private final NotificationService notificationService;
  private final PersonService personService;
  private final NegotiationRepository negotiationRepository;

  NewPostHandler(
      NotificationService notificationService,
      PersonService personService,
      NegotiationRepository negotiationRepository) {
    this.notificationService = notificationService;
    this.personService = personService;
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  public Class<NewPostEvent> getSupportedEventType() {
    return NewPostEvent.class;
  }

  @Override
  @Transactional
  public void notify(NewPostEvent event) {
    if (event.getOrganizationId() == null) {
      forPublicPost(event);
    } else {
      forPrivatePost(event);
    }
  }

  private void forPublicPost(NewPostEvent event) {
    Negotiation negotiation =
        negotiationRepository
            .findById(event.getNegotiationId())
            .orElseThrow(() -> new EntityNotFoundException(event.getNegotiationId()));

    // Get all representatives and the negotiation author (researcher)
    Set<Person> recipients = new HashSet<>();

    // Add all resource representatives
    negotiation.getResources().stream()
        .flatMap(resource -> resource.getRepresentatives().stream())
        .forEach(recipients::add);

    // Add the negotiation author (researcher)
    recipients.add(negotiation.getCreatedBy());

    // Always exclude the post author from notifications
    recipients.removeIf(person -> person.getId().equals(event.getUserId()));

    if (!recipients.isEmpty()) {
      notificationService.createNotifications(
          new NotificationCreateDTO(
              recipients.stream().map(Person::getId).toList(),
              TITLE,
              BODY + negotiation.getTitle(),
              event.getNegotiationId()));
    }
  }

  private void forPrivatePost(NewPostEvent event) {
    // Get all users from the organization
    List<Long> orgUserIds =
        personService.findAllByOrganizationId(event.getOrganizationId()).stream()
            .map(user -> Long.valueOf(user.getId()))
            .toList();
    Negotiation negotiation =
        negotiationRepository
            .findById(event.getNegotiationId())
            .orElseThrow(() -> new EntityNotFoundException(event.getNegotiationId()));
    List<Long> recipientIds = new ArrayList<>(orgUserIds);
    recipientIds.add(negotiation.getCreatedBy().getId());
    recipientIds.remove(event.getUserId());

    if (!recipientIds.isEmpty()) {
      notificationService.createNotifications(
          new NotificationCreateDTO(
              recipientIds, TITLE, BODY + negotiation.getTitle(), event.getNegotiationId()));
    }
  }
}
