package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class UpdatedResourcesHandler implements NotificationStrategy<NewResourcesAddedEvent> {

  private final ResourceNotificationService resourceNotificationService;

  public UpdatedResourcesHandler(ResourceNotificationService resourceNotificationService) {
    this.resourceNotificationService = resourceNotificationService;
  }

  @Override
  public Class<NewResourcesAddedEvent> getSupportedEventType() {
    return NewResourcesAddedEvent.class;
  }

  @Override
  @Transactional
  public void notify(NewResourcesAddedEvent event) {
    resourceNotificationService.notifyResourceRepresentatives(event.getNegotiationId());
  }
}
