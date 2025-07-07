package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.AdminNotificationService;
import eu.bbmri_eric.negotiator.notification.RepresentativeNotificationService;
import eu.bbmri_eric.negotiator.notification.ResearcherNotificationService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
class NegotiationStateChangeHandler implements NotificationStrategy<NegotiationStateChangeEvent> {
  private final ResearcherNotificationService researcherNotificationService;
  private final AdminNotificationService adminNotificationService;
  private final RepresentativeNotificationService representativeNotificationService;

  public NegotiationStateChangeHandler(
      ResearcherNotificationService researcherNotificationService,
      AdminNotificationService adminNotificationService,
      RepresentativeNotificationService representativeNotificationService) {
    this.researcherNotificationService = researcherNotificationService;
    this.adminNotificationService = adminNotificationService;
    this.representativeNotificationService = representativeNotificationService;
  }

  @Override
  public Class<NegotiationStateChangeEvent> getSupportedEventType() {
    return NegotiationStateChangeEvent.class;
  }

  @Override
  public void notify(NegotiationStateChangeEvent event) {
    switch (event.getChangedTo()) {
      case SUBMITTED -> {
        researcherNotificationService.createConfirmationNotification(event.getNegotiationId());
        adminNotificationService.notifyAllAdmins(
            "New Request", "A new Request has been submitted for review", event.getNegotiationId());
      }
      case IN_PROGRESS ->
          representativeNotificationService.notifyAboutANewNegotiation(event.getNegotiationId());
    }
  }
}
