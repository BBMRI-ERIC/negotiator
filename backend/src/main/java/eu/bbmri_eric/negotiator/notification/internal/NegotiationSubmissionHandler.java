package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

/** Handles notifications when negotiations are submitted for admin review. */
@Component
@CommonsLog
class NegotiationSubmissionHandler implements NotificationStrategy<NegotiationStateChangeEvent> {

  public static final String TITLE = "New Request";
  public static final String BODY = "A new Request has been submitted for review";
  private final NotificationService notificationService;
  private final PersonRepository personRepository;

  NegotiationSubmissionHandler(
      NotificationService notificationService, PersonRepository personRepository) {
    this.notificationService = notificationService;
    this.personRepository = personRepository;
  }

  @Override
  public Class<NegotiationStateChangeEvent> getSupportedEventType() {
    return NegotiationStateChangeEvent.class;
  }

  @Override
  @Transactional
  public void notify(NegotiationStateChangeEvent event) {
    if (event.getToState() == NegotiationState.SUBMITTED) {
      notifyAdminsAboutNewSubmission(event.getNegotiationId());
    }
  }

  private void notifyAdminsAboutNewSubmission(String negotiationId) {
    List<Long> adminIds =
        personRepository.findAllByAdminIsTrue().stream().map(Person::getId).toList();

    if (!adminIds.isEmpty()) {
      NotificationCreateDTO notification =
          new NotificationCreateDTO(adminIds, TITLE, BODY, negotiationId);

      notificationService.createNotifications(notification);
      log.info(
          "Notified "
              + adminIds.size()
              + " administrators about new negotiation submission: "
              + negotiationId);
    }
  }
}
