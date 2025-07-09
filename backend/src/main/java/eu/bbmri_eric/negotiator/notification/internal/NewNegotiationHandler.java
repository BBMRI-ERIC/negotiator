package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
class NewNegotiationHandler implements NotificationStrategy<NewNegotiationEvent> {
  private final NotificationService notificationService;
  private final NegotiationRepository negotiationRepository;
  private final PersonRepository personRepository;

  public NewNegotiationHandler(
      NotificationService notificationService,
      NegotiationRepository negotiationRepository,
      PersonRepository personRepository) {
    this.notificationService = notificationService;
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
  }

  @Override
  public Class<NewNegotiationEvent> getSupportedEventType() {
    return NewNegotiationEvent.class;
  }

  @Override
  @Transactional
  public void notify(NewNegotiationEvent event) {
    createResearcherConfirmationNotification(event.getNegotiationId());
    notifyAdminsAboutNewNegotiation(event.getNegotiationId());
  }

  private void createResearcherConfirmationNotification(String negotiationId) {
    Negotiation negotiation = negotiationRepository.findById(negotiationId).orElse(null);
    if (negotiation == null) {
      log.warn("Could not find negotiation with ID: " + negotiationId);
      return;
    }

    NotificationCreateDTO notification =
        new NotificationCreateDTO(
            List.of(negotiation.getCreatedBy().getId()),
            "Negotiation Created Successfully",
            "Your negotiation request has been created successfully and will be reviewed by administrators.",
            negotiationId);

    notificationService.createNotifications(notification);
    log.info(
        "Sent creation confirmation notification to researcher for negotiation: " + negotiationId);
  }

  private void notifyAdminsAboutNewNegotiation(String negotiationId) {
    List<Long> adminIds =
        personRepository.findAllByAdminIsTrue().stream().map(Person::getId).toList();

    if (!adminIds.isEmpty()) {
      NotificationCreateDTO notification =
          new NotificationCreateDTO(
              adminIds,
              "New Request",
              "A new Request has been submitted for review",
              negotiationId);

      notificationService.createNotifications(notification);
      log.info(
          "Notified "
              + adminIds.size()
              + " administrators about new negotiation: "
              + negotiationId);
    }
  }
}
