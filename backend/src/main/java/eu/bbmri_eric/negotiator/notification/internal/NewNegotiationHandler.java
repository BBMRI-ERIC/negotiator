package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.notification.AdminNotificationService;
import eu.bbmri_eric.negotiator.notification.ResearcherNotificationService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
class NewNegotiationHandler implements NotificationStrategy<NewNegotiationEvent> {
  private final ResearcherNotificationService researcherNotificationService;
  private final AdminNotificationService adminNotificationService;

  public NewNegotiationHandler(
      ResearcherNotificationService researcherNotificationService,
      AdminNotificationService adminNotificationService) {
    this.researcherNotificationService = researcherNotificationService;
    this.adminNotificationService = adminNotificationService;
  }

  @Override
  public Class<NewNegotiationEvent> getSupportedEventType() {
    return NewNegotiationEvent.class;
  }

  @Override
  public void notify(NewNegotiationEvent event) {
    researcherNotificationService.createConfirmationNotification(event.getNegotiationId());
    adminNotificationService.notifyAllAdmins(
        "New Request", "A new Request has been submitted for review", event.getNegotiationId());
  }
}
