package eu.bbmri_eric.negotiator.listeners;

import eu.bbmri_eric.negotiator.events.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.service.UserNotificationService;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class NewNegotiationEventListener implements ApplicationListener<NewNegotiationEvent> {
  private final UserNotificationService userNotificationService;

  public NewNegotiationEventListener(UserNotificationService userNotificationService) {
    this.userNotificationService = userNotificationService;
  }

  @Override
  public void onApplicationEvent(NewNegotiationEvent event) {
    userNotificationService.notifyRepresentativesAboutNewNegotiation(event.getNegotiationId());
  }
}
