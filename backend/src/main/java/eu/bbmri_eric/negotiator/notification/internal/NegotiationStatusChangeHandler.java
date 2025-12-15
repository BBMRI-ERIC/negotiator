package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.settings.AdminSettingsRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

/** Handles notifications when negotiation status changes, informing researchers of updates. */
@Component
@CommonsLog
class NegotiationStatusChangeHandler implements NotificationStrategy<NegotiationStateChangeEvent> {

  public static final String TITLE = "Negotiation Submission Confirmed";
  public static final String BODY =
      "Your negotiation request has been successfully submitted and is now under review. You will be notified of any updates.";
  private final NotificationService notificationService;
  private final NegotiationRepository negotiationRepository;
  private final AdminSettingsRepository adminSettingsRepository;
  private final PersonRepository personRepository;

  NegotiationStatusChangeHandler(
      NotificationService notificationService,
      NegotiationRepository negotiationRepository,
      AdminSettingsRepository adminSettingsRepository,
      PersonRepository personRepository) {
    this.notificationService = notificationService;
    this.negotiationRepository = negotiationRepository;
    this.adminSettingsRepository = adminSettingsRepository;
    this.personRepository = personRepository;
  }

  @Override
  public Class<NegotiationStateChangeEvent> getSupportedEventType() {
    return NegotiationStateChangeEvent.class;
  }

  @Override
  @Transactional
  public void notify(NegotiationStateChangeEvent event) {
    switch (event.getChangedTo()) {
      case SUBMITTED -> createConfirmationNotification(event.getNegotiationId());
      case IN_PROGRESS, DECLINED, ABANDONED -> createStatusChangeNotification(event);
    }
  }

  private void createConfirmationNotification(String negotiationId) {
    Negotiation negotiation = negotiationRepository.findById(negotiationId).orElse(null);
    if (negotiation == null) {
      log.warn("Could not find negotiation with ID: " + negotiationId);
      return;
    }

    NotificationCreateDTO notification =
        new NotificationCreateDTO(
            List.of(negotiation.getCreatedBy().getId()), TITLE, BODY, negotiationId);

    notificationService.createNotifications(notification);
    log.info("Sent confirmation notification to researcher for negotiation: " + negotiationId);
  }

  private void createStatusChangeNotification(NegotiationStateChangeEvent event) {
    Negotiation negotiation = negotiationRepository.findById(event.getNegotiationId()).orElse(null);
    if (negotiation == null) {
      log.warn("Could not find negotiation with ID: " + event.getNegotiationId());
      return;
    }
    String title = "Request Status Update";
    String message = createStatusChangeMessage(event.getChangedTo(), negotiation.getTitle());

    NotificationCreateDTO notification =
        new NotificationCreateDTO(
            List.of(negotiation.getCreatedBy().getId()), title, message, event.getNegotiationId());

    notificationService.createNotifications(notification);
    log.info(
        "Sent status change notification to researcher for negotiation: "
            + event.getNegotiationId()
            + " - new status: "
            + event.getChangedTo());

    if (adminSettingsRepository.getSendNegotiationUpdatesNotifications()) {
      List<Person> administrators = personRepository.findAllByAdminIsTrue();
      String adminNotificationTitle =
          "Admin notification update for negotiation: " + negotiation.getId();
      String body =
          "There are updates on negotiation "
              + negotiation.getId()
              + ".\n"
              + "Please log into the Negotiator for more details.";
      notificationService.createNotifications(
          new NotificationCreateDTO(
              administrators.stream().map(Person::getId).toList(),
              adminNotificationTitle,
              body,
              event.getNegotiationId()));
    }
  }

  private String createStatusChangeMessage(NegotiationState newState, String negotiationTitle) {
    return switch (newState) {
      case IN_PROGRESS ->
          "Your negotiation request '"
              + negotiationTitle
              + "' has been approved. You can now proceed with the next steps.";
      case DECLINED ->
          "Your negotiation request '"
              + negotiationTitle
              + "' has been rejected. Please review the feedback and consider resubmitting with modifications.";
      case ABANDONED ->
          "Your negotiation request '" + negotiationTitle + "' has been marked as abandoned.";
      default ->
          "Your negotiation request '"
              + negotiationTitle
              + "' status has been updated to: "
              + newState;
    };
  }
}
