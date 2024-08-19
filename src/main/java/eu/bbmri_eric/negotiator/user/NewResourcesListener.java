package eu.bbmri_eric.negotiator.listeners;

import eu.bbmri_eric.negotiator.events.NewResourcesAddedEvent;
import eu.bbmri_eric.negotiator.service.UserNotificationService;
import lombok.NonNull;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NewResourcesListener implements ApplicationListener<NewResourcesAddedEvent> {
  UserNotificationService notificationService;

  public NewResourcesListener(UserNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Override
  @Async
  public void onApplicationEvent(@NonNull NewResourcesAddedEvent event) {
    notificationService.notifyRepresentativesAboutNewNegotiation(event.getNegotiationId());
  }
}
